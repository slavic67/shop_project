package com.project.order.domain.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class OrderCreatedEvent(
    val orderId: Long,
    val mdcContext: Map<String, String>? = null,
    val timestamp: Instant,
) {
    companion object {
        fun of(orderId: Long, mdcContext: Map<String, String>?)=
            OrderCreatedEvent(orderId, mdcContext, Instant.now())
    }
}