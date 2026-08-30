package com.project.order

import java.math.BigDecimal
import kotlin.time.Instant

data class OrderResponse(
    val id: Long,
    val status: OrderStatus?,
    val createdAt: Instant?,
    val items: List<OrderItemResponse>,
    val total: BigDecimal
) {
}