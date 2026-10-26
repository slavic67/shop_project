package com.payment.domain.event

data class PaymentFailedEvent(
    val orderId: Long,
    val reason: String
)
