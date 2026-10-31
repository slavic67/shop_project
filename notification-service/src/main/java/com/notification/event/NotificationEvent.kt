package com.notification.event

data class NotificationEvent(
    val orderId: Long?,
    val transactionId: String?,
    val status: String,
    val reason: String?
) {
}