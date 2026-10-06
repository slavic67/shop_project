package com.project.order.service

import com.project.order.domain.Order
import com.project.order.domain.OrderItem
import com.project.order.domain.dto.CreateOrderRequest
import com.project.order.domain.dto.OrderCreatedEvent
import com.project.order.domain.dto.OrderResponse
import com.project.order.domain.dto.PaymentResponseDto
import com.project.order.exception.NotFoundOrderException
import com.project.order.mapper.OrderMapper
import com.project.order.metrics.annotation.BusinessMetric
import com.project.order.repository.OrderRepository
import io.micrometer.observation.annotation.Observed
import io.opentelemetry.api.trace.Span
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean


@Slf4j
@Service
@RequiredArgsConstructor
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderMapper: OrderMapper,
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentService: PaymentService
) {
    private val random = Random()
    private val failureMode: AtomicBoolean = AtomicBoolean(false)

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @BusinessMetric(
        value = "orders.created",
        tags = ["operation=create", "type=write"]
    )
    @Observed(name="order.creation", contextualName = "create-order")
    fun createOrder(request: CreateOrderRequest): OrderResponse? {

        log.debug("В метод createOrder получен запрос: {}", request)

        if (failureMode.get()) {

            val randomInt: Int = random.nextInt(100)

            log.info("Выпало число: $random")

            if (randomInt < 30) {
                log.error("Возникли проблемы с сохранением заказа")
                throw RuntimeException("Ошибка соединения с бд")
            }

            if (randomInt > 70) {
                Thread.sleep(200)
            }

        }


        val items :List<OrderItem> = request.items.stream()
            .map {
                OrderItem(
                    it.productId,
                    it.productName,
                    it.quantity,
                    it.price
                )
            }
            .toList()

        val order : Order = Order(items)
        val savedOrder = orderRepository.saveAndFlush(order)

        val response: PaymentResponseDto? = paymentService.processPaymentResilience4j(savedOrder)

        if (response!=null && !response.isSuccessful) {
            log.warn("Бизнес-ошибка оплаты: ${response.message}")
        }

        log.info("Оплата заказа id: ${savedOrder.id}, успешно проведена")

        log.debug("Отправляем инфо о заказе, id : ${savedOrder.id}")

        try {
            MDC.put("order_id", savedOrder.id.toString());
            MDC.put("total_amount", savedOrder.calculateTotalAmount().toString());
            MDC.put("order_status", savedOrder.status?.name);

            eventPublisher.publishEvent(
                OrderCreatedEvent.of(
                savedOrder.id!!,
                MDC.getCopyOfContextMap()
                )
            )

            log.debug("Заказ успешно сохранен")

            // Теги добавятся в order.creation span
            Span.current().setAttribute("orderId", order.id!!)

            return orderMapper.from(order)
        } finally {
            // Очищаем только свои поля, trace_id остаётся чтобы не засорять память
            MDC.remove("order_id");
            MDC.remove("total_amount");
            MDC.remove("order_status");
        }
    }

    @Transactional(readOnly = true)
    @BusinessMetric(
        value = "orders.retrieved",
        tags = ["operation=get", "type=read"]
    )
    @Observed(name="order.get", contextualName = "get-order")
    fun getOrderWithItems(id: Long) : OrderResponse {


        log.debug("В метод getOrderWithItems получен запрос поиска order по id: {}", id)

        val order = orderRepository.findWithItemsById(id).orElseThrow(
            { NotFoundOrderException("Order not found for id $id") },
        )

        log.debug("Результат успешно найден")
        return orderMapper.from(order)

    }

    fun setFailureMode(enabled: Boolean) {
            failureMode.set(enabled)
            log.info("Failure mode в OrderService, переключен на: $enabled")
    }
}