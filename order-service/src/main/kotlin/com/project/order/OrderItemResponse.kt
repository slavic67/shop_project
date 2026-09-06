package com.project.order

import java.math.BigDecimal

data class OrderItemResponse(
    val productId: Long?,
    val productName: String?,
    val quantity: Int,
    val price: BigDecimal?,
    val itemTotal: BigDecimal?
) {
}