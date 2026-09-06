package com.project.order.domain.dto

import java.time.LocalDateTime

data class OrderCreatedEvent(
    val orderId: Long,
    val timestamp: LocalDateTime,
) {
    companion object {
        fun of(orderId: Long)=OrderCreatedEvent(orderId, LocalDateTime.now())
    }
}