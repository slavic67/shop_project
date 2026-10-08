package com.project.order.service

import com.project.order.client.PaymentClient
import com.project.order.domain.Order
import com.project.order.domain.dto.PaymentRequestDto
import com.project.order.domain.dto.PaymentResponseDto
import com.project.order.exception.variants.PaymentFailedException
import com.project.order.feignClient.PaymentFeignClient
import feign.FeignException
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.retry.RetryContext
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.support.RetrySynchronizationManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

@Service
@RequiredArgsConstructor
class PaymentService(
    private val paymentClient: PaymentClient,
    private val paymentFeignClient: PaymentFeignClient
) {

    private val log = LoggerFactory.getLogger(PaymentService::class.java)

        //стандарт спринг, при использовании @CircuitBreaker, @Retryable не использовать
        @Retryable(
            // При каких исключениях повторять (работает по instanceof)
            retryFor = [WebClientResponseException::class, PaymentFailedException::class],

            // При каких исключениях НЕ повторять (даже если подходят под retryFor)
            noRetryFor = [IllegalArgumentException::class],

            // Максимальное количество попыток (включая первую)
            maxAttempts = 3,

//            // SpEL-выражение для динамического задания maxAttempts (например, из property)
//            maxAttemptsExpression = $$"#{${retry.max-attempts}}",

            // Настройка задержки между попытками
            backoff = Backoff(
                delay = 200,          // начальная задержка (мс)
                maxDelay = 5000,       // максимальная задержка (ограничитель)
                multiplier = 2.0,      // множитель для экспоненциального роста
                random = true          // jitter — случайное отклонение
            ),

//            // SpEL-выражение для тонкой фильтрации исключений (доступен #root)
//            exceptionExpression = "#{@exceptionChecker.shouldRetry(#root)}",
//
//            // Сохранять состояние между попытками (нужно для circuit breaker)
//            stateful = false,
//
//            // Метка для мониторинга и логирования
//            label = "",
//
//            // Имена Spring-бинов, реализующих RetryListener
//            listeners = [""],
//
//            // Имя метода fallback, вызываемого после исчерпания попыток
//            recover = "",
//
//            // Исключения, при которых fallback НЕ вызывается
//            notRecoverable = [],
//
//            // interceptor — имя бина MethodInterceptor для кастомной логики (редко используется)
//            interceptor = ""
        )
//        @CircuitBreaker(
//            // ----- Параметры, унаследованные от @Retryable (через @AliasFor) -----
//            retryFor = [PaymentFailedException::class],      // Какие исключения считать сбоем
//            noRetryFor = [IllegalArgumentException::class],  // Какие исключения игнорировать
//            maxAttempts = 5,                                // После скольких сбоев подряд breaker откроется
//            label = "",                                     // Метка для мониторинга
//            recover = "",                                   // Метод fallback
//            notRecoverable = [],                            // Исключения, при которых fallback не вызывается
//
//            // ----- Специфичные параметры circuit breaker -----
//            resetTimeout = 30000,                                   // Время (мс) в OPEN до перехода в HALF-OPEN
//            resetTimeoutExpression = $$"#{${breaker.reset-timeout}}", // SpEL для resetTimeout
//            openTimeout = 5000,                                     // Таймаут нахождения в OPEN (редко используется)
//            openTimeoutExpression = $$"#{${breaker.open-timeout}}",
//            throwLastExceptionOnExhausted = false                   // Выбрасывать ли исключение после исчерпания
//    )
    fun processPaymentStandardSpring(savedOrder: Order) {

        val context: RetryContext? = RetrySynchronizationManager.getContext()
        val attempt: Int = context?.retryCount ?: 1

        log.debug("Попытка $attempt из 3 <===")

        try {

        } catch (e: WebClientResponseException) {
            log.error("Попытка {}: Техническая ошибка при вызове payment-service {}",attempt, e.statusCode)

            throw PaymentFailedException("Payment service error: " + e.statusText)
        }

    }

    //для асинхронного взаимодействия должен возвращать CompletableFuture<PaymentResponseDto>
    @CircuitBreaker(
        name = "paymentService",  //Имя экземпляра, должно соответствовать имени в конфигурации
        fallbackMethod = "asyncPaymentFallback" //Метод, который вызовется при открытом breaker'e или исключении
    )
    @TimeLimiter(name = "paymentService")
    @Retry(name = "paymentService")
    @Bulkhead(name="paymentService")
    fun processPaymentResilience4j(savedOrder: Order): CompletableFuture<PaymentResponseDto?> {

        try {

            return  paymentClient
                .processPayment(savedOrder.id!!, savedOrder.getTotalPrice())
                .toFuture()

        } catch (e: WebClientResponseException) {
            log.error("Техническая ошибка при вызове payment-service")
            throw PaymentFailedException("Payment service error: ${e.statusText}")
        }
    }


    @CircuitBreaker(
        name = "paymentService",  //Имя экземпляра, должно соответствовать имени в конфигурации
        fallbackMethod = "syncPaymentFallback" //Метод, который вызовется при открытом breaker'e или исключении
    )
    @Retry(name = "paymentService")
    @Bulkhead(name="paymentService", fallbackMethod = "syncPaymentFallback")
    fun processPaymentFeign(savedOrder: Order): PaymentResponseDto? {

        try {
            val response= paymentFeignClient
                .processPayment(
                    PaymentRequestDto(savedOrder.id!!, savedOrder.getTotalPrice())
                )

            log.debug("Успешно обработана оплата через feign-клиента. Ответ: {}", response)

            return response

        } catch (e: FeignException.FeignClientException) {
            log.error("Техническая ошибка при вызове payment-service через feign-клиента")
            throw PaymentFailedException("Payment service error: ${e.message}")
        }
    }



    @Recover
    fun asyncPaymentFallback(savedOrder: Order, t: Throwable):
            CompletableFuture<PaymentResponseDto?> {
        return CompletableFuture.completedFuture(
            syncPaymentFallback(savedOrder, t)
        )
    }

    fun syncPaymentFallback(savedOrder: Order, t: Throwable): PaymentResponseDto? {
        log.warn("Fallback для заказа: {}, по причине: {}", savedOrder.id, t.message);

        val userMessage: String =getUserMessage(t)

        return PaymentResponseDto(
                isSuccessful = true,
                requiresPendingProcessing = true,
                message = userMessage
            )
    }


    private fun getUserMessage(t: Throwable): String {
        if (t is TimeoutException) {
            return "Платежный сервис временно недоступен. Заказ будет обработан автоматически."
        } else if (t is CallNotPermittedException) {
            return "Сервис оплаты перегружен. Ваш заказ в очереди на обработку"
        }
        return "Заказ принят. Статус оплаты будет обновлен позже"
    }

}