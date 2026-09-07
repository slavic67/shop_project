package com.notification

data class NotificationRequest(
    val orderId: Long,
    val eventType: String,
)
