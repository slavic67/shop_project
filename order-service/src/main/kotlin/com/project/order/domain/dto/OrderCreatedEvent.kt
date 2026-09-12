package com.project.order.domain.dto

import java.time.LocalDateTime

data class OrderCreatedEvent(
    val orderId: Long,
    val mdcContext: Map<String, String>? = null,
    val timestamp: LocalDateTime,
) {
    companion object {
        fun of(orderId: Long, mdcContext: Map<String, String>?)=
            OrderCreatedEvent(orderId, mdcContext, LocalDateTime.now())
    }
}