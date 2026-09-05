package com.project.order

import com.project.order.exception.NotFoundOrderException
import com.project.order.repository.OrderRepository

import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@RequiredArgsConstructor
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderMapper: OrderMapper
) {

    @Transactional
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
        return orderMapper.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrderWithItems(id: Long) : OrderResponse {
        val order = orderRepository.findWithItemsById(id).orElseThrow(
            { NotFoundOrderException("Order not found for id $id") },
        )
        return orderMapper.from(order)

    }
}