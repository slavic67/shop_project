package com.project.order

import com.project.order.exception.NotFoundOrderException
import com.project.order.metrics.annotation.BusinessMetric
import com.project.order.repository.OrderRepository
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Slf4j
@Service
@RequiredArgsConstructor
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderMapper: OrderMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @BusinessMetric(
        value = "orders.created",
        tags = ["operation=create", "type=write"]
    )
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

        val order : Order= Order(items)
        orderRepository.save(order)
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