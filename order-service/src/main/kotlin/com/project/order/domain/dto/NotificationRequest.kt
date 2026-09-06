package com.project.order.domain.dto

data class NotificationRequest(
    val orderId: Long,
    val eventType: String
) {
}