package com.project.order.domain.event

data class PaymentFailedEvent(
    val orderId: Long,
    val reason: String
)
