package com.project.order.domain.event

data class PaymentCompletedEvent(
    val orderId: Long,
    val transactionId: String,
    val status: String
)
