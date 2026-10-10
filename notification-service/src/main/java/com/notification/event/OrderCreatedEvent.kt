package com.notification.event

import java.time.Instant
import kotlin.time.Clock


data class OrderCreatedEvent(
    val orderId: Long,
    val mdcContext: Map<String, String?> =emptyMap(),
    val timestamp: Instant
) {
    companion object {
        fun of(orderId: Long, context: Map<String, String?>): OrderCreatedEvent {
            return OrderCreatedEvent(
                orderId = orderId,
                mdcContext = context,
                timestamp = Instant.now()
            )
        }
    }

}