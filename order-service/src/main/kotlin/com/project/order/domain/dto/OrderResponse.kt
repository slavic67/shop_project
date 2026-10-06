package com.project.order.domain.dto

import com.project.order.dictionary.OrderStatus
import java.math.BigDecimal
import java.time.Instant

data class OrderResponse(
    val id: Long,
    val status: OrderStatus?,
    val createdAt: Instant?,
    val items: List<OrderItemResponse>,
    val total: BigDecimal
) {
}