package com.project.order.domain.command

data class CancelOrderCommand(
    val orderId: Long,
    val reason: String
)
