package com.payment.domain.event

data class PaymentCompletedEvent(
    val orderId: Long,
    val transactionId: String,
    val status: String
)
