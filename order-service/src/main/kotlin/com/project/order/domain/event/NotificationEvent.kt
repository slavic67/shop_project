package com.project.order.domain.event

data class NotificationEvent(
    val orderId: Long,
    val transactionId: String?,
    val status: String,
    val reason: String?
)
