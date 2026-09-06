package com.project.order

import com.project.order.domain.Order
import com.project.order.domain.OrderItem
import com.project.order.domain.dto.OrderCreatedEvent
import com.project.order.exception.NotFoundOrderException
import com.project.order.metrics.annotation.BusinessMetric
import com.project.order.repository.OrderRepository
import io.micrometer.observation.annotation.Observed
import io.opentelemetry.api.trace.Span
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Slf4j
@Service
@RequiredArgsConstructor
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderMapper: OrderMapper,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @BusinessMetric(
        value = "orders.created",
        tags = ["operation=create", "type=write"]
    )
    @Observed(name="order.creation", contextualName = "create-order")
    fun createOrder(request: CreateOrderRequest): OrderResponse? {

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
        orderRepository.saveAndFlush(order)

        log.info("Отправляем инфо о заказе, id: ${order.id}")

        eventPublisher.publishEvent(OrderCreatedEvent.of(order.id!!))

        log.debug("Заказ успешно сохранен")

        // Теги добавятся в order.creation span
        Span.current().setAttribute("orderId", order.id!!)

        return orderMapper.from(order)
    }

    @Transactional(readOnly = true)
    @BusinessMetric(
        value = "orders.retrieved",
        tags = ["operation=get", "type=read"]
    )
    fun getOrderWithItems(id: Long) : OrderResponse {


        log.debug("В метод getOrderWithItems получен запрос поиска order по id: {}", id)

        val order = orderRepository.findWithItemsById(id).orElseThrow(
            { NotFoundOrderException("Order not found for id $id") },
        )

        log.debug("Результат успешно найден")
        return orderMapper.from(order)

    }
}