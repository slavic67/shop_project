package com.notification.domain.command

data class UpdateOrderStatusCommand(
    val orderId: Long,
    val transactionId: String,
    val newStatus: String
) {}
