package com.project.order.domain.command

data class UpdateOrderStatusCommand(
    val orderId: Long,
    val transactionId: String,
    val newStatus: String
)
