package com.notification.domain.command

data class CancelOrderCommand(
    val orderId: Long,
    val reason: String
) {}
