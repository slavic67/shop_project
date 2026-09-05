package com.project.order.demo

import com.project.order.CreateOrderRequest
import com.project.order.Order
import com.project.order.OrderItem
import com.project.order.OrderMapper
import com.project.order.OrderResponse
import com.project.order.demo.repository.JdbcOrderRepository
import com.project.order.demo.repository.JooqOrderRepository
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * ДЕМОНСТРАЦИОННЫЙ СЕРВИС
 */
@Service
@RequiredArgsConstructor
class DemoOrderService(
    private val jdbcRepository: JdbcOrderRepository,
    private val jooqRepository: JooqOrderRepository,
    private val orderMapper: OrderMapper
) {

    @Transactional
    fun createWithJdbc(request: CreateOrderRequest): OrderResponse {
        val items: List<OrderItem> = request.items.stream()
            .map{ item -> OrderItem(
                item.productId,
                item.productName,
                item.quantity,
                item.price,
            )}.toList()

        val order: Order = Order(items)
        val savedOrder = jdbcRepository.save(order)
        return orderMapper.from(savedOrder)
    }

    fun createWithJooq(request: CreateOrderRequest): OrderResponse {
        val items: List<OrderItem> = request.items.stream()
            .map { item -> OrderItem(
                item.productId,
                item.productName,
                item.quantity,
                item.price,
            ) }.toList()

        val order: Order = Order(items)
        val savedOrder = jooqRepository.save(order)
        return orderMapper.from(savedOrder)
    }


}