package com.project.order

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderMapper {

    fun from(item: OrderItem): OrderItemResponse =
        OrderItemResponse(
            item.id,
            item.productName,
            item.quantity,
            item.price,
            item.price?.multiply(BigDecimal(item.quantity))
        )

    fun from(order: Order) : OrderResponse {

        val total: BigDecimal = order.items.stream()
            .map { it.price!!.multiply(BigDecimal.valueOf(it.quantity.toLong())) }
            .reduce(BigDecimal.ZERO, BigDecimal::add)

        val items: List<OrderItemResponse> = order.items.stream()
            .map { this.from(it) }.toList()

        return OrderResponse(
            order.id!!,
            order.status,
            order.createAt,
            items,
            total
        )
    }
}