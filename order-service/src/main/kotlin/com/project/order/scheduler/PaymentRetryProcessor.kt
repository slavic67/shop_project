package com.project.order.scheduler

import com.project.order.dictionary.OrderStatus
import com.project.order.domain.Order
import com.project.order.domain.dto.OrderCreatedEvent
import com.project.order.domain.dto.PaymentResponseDto
import com.project.order.repository.OrderRepository
import com.project.order.service.PaymentService
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.MDC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class PaymentRetryProcessor(

    @Value($$"${payment.retry.max-attempts:3}")
    private var maxAttempts: Int,

    @Value($$"${payment.retry.timeout:30}")
    private var timeoutSeconds: Long,

    @Value($$"${payment.retry.batch-size:500}")
    private var batchSize: Int,

    private val paymentService: PaymentService,
    private val orderRepository: OrderRepository,
    private val eventPublisher: ApplicationEventPublisher,

    //для метрик
    private val meterRegistry: MeterRegistry,
) {

    private val retryCounters: MutableMap<Long, Int> = ConcurrentHashMap()
    private val log = LoggerFactory.getLogger(PaymentRetryProcessor::class.java)

    @Transactional
    fun processOrder(order: Order) {

        // Проверяем, не превышен ли лимит попыток
        val attempts: Int = retryCounters.getOrDefault(order.id, 0)

        if (attempts >= maxAttempts) {
            log.debug("Заказ {} исчерпал все попытки ({}). Отмена.", order.id, maxAttempts)

            order.status = OrderStatus.PENDING
            orderRepository.save(order)

            retryCounters.remove(order.id)

            recordMaxAttemptsExceededMetrics()

            return
        }

        // Проверяем, не устарел ли заказ (например, старше 1 часа)
        if (isOrderExpired(order)) {
            log.warn("Заказ {} устарел (>1 часа). Отмена.", order.id)

            order.status = OrderStatus.CANCELLED
            orderRepository.save(order)

            retryCounters.remove(order.id)
            recordExpiredMetrics()

            return
        }

        //Пытаемся оплатить
        log.debug("Попытка {}/{} оплаты заказа {}", attempts +1 ,maxAttempts, order.id)

        try {
            val response: PaymentResponseDto = paymentService.processPaymentResilience4j(order)
                .get(timeoutSeconds, TimeUnit.SECONDS)!!

            if (response.requiresPendingProcessing) {
                // Сервис недоступен - увеличиваем счетсчик попыток
                retryCounters[order.id!!] = attempts +1
                log.warn("Заказ {} не оплачен (попытка {}). Сервис недоступен.", order.id, attempts+1)

                recordPendingMetrics(attempts+1)
            } else if (response.isSuccessful) {

                try {
                    //Успех!
                    MDC.put("order_id", order.id.toString())
                    MDC.put("total_amount", order.getTotalPrice().toString())
                    MDC.put("order_status", order.status.toString())

                    order.status= OrderStatus.PAID
                    val savedOrder: Order = orderRepository.save(order)

                    log.info("Заказ {} успшно оплачен с {}-й попытки", order.id, attempts+1)

                    retryCounters.remove(order.id)
                    recordSuccessMetrics(attempts+1)

                    eventPublisher.publishEvent(
                        OrderCreatedEvent.of(
                            orderId = savedOrder.id!!,
                            mdcContext = MDC.getCopyOfContextMap()
                        )
                    )


                } finally {
                    MDC.remove("order_id")
                    MDC.remove("total_amount")
                    MDC.remove("order_status")
                }

            } else {
                // Бизнес-ошибка (недостаточно средств и т.п.)
                order.status= OrderStatus.CANCELLED
                orderRepository.save(order)

                log.warn("Заказ {} отклонен: {}", order.id, response.message)

                retryCounters.remove(order.id)
                recordBusinessErrorMetrics(response.message)
            }
        } catch (e: TimeoutException) {
            // Таймаут при вызове
            retryCounters[order.id!!] = attempts+1
            log.error("Таймаут для заказа {} (попытка {}): {}",order.id, attempts+1, e.message)

            // МЕТРИКА: техническая ошибка (таймаут)
            recordTechnicalErrorMetrics(attempts+1, "TIMEOUT")

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            retryCounters[order.id!!] = attempts+1
            log.warn("Обработка заказа {} прервана (попытка {})", order.id, attempts+1)

            //МЕТРИКА: техническая ошибка (прерывание)
            recordTechnicalErrorMetrics(attempts+1, "INTERRUPTED")

        } catch (e: ExecutionException) {
            retryCounters[order.id!!] = attempts+1
            val cause: Throwable = e.cause ?: e
            log.error("Ошибка выполнения для заказа {} (попытка {}): {}",order.id, attempts+1, e.message)

            //МЕТРИКА: техническая ошибка (выполнение)
            recordTechnicalErrorMetrics(attempts + 1, "EXECUTION");
        } catch (e: Exception) {
            log.error("Неожиданная ошибка для заказа {}", order.id, e)
            retryCounters[order.id!!] = attempts+1

            //Метрика: техническая ошибка (неизвестно)
            recordTechnicalErrorMetrics(attempts+1, "UNKNOWN")
        }

    }

    fun isOrderExpired(order: Order): Boolean {
        // Заказы старше 1 часа считаем устаревшими
        return order.createAt.plus(1, TimeUnit.HOURS.toChronoUnit()).isBefore(Instant.now())
    }

    private fun recordTechnicalErrorMetrics(attempts: Int, errorType: String) {
        meterRegistry.counter(
            "orders.payment.retry.technical_error_total",
            "attempts", attempts.toString() , "error_type", errorType
        ).increment()
    }

    private fun recordSuccessMetrics(attempts: Int) {
        meterRegistry.counter(
            "orders.payment.retry.success_total",
            "attempts", attempts.toString()
        ).increment()

        meterRegistry.summary("orders.payment.retry.attempts").record(attempts.toDouble())
    }

    private fun recordPendingMetrics(attempts: Int) {
        meterRegistry.counter(
            "orders.payment.retry.pending_total",
            "attempts", attempts.toString()
        ).increment()
    }

    private fun recordMaxAttemptsExceededMetrics() {
        meterRegistry.counter(
            "orders.payment.retry.max_attempts_exceeded_total"
        ).increment()
    }

    private fun recordExpiredMetrics() {
        meterRegistry.counter(
            "orders.payment.retry.expired_total"
        ).increment()
    }

    private fun recordBusinessErrorMetrics(errorMessage: String) {
        meterRegistry.counter(
            "orders.payment.retry.business_error_total",
            "error", errorMessage
        ).increment()
    }

}