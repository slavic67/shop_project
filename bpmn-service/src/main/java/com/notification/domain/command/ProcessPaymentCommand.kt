package com.notification.domain.command

import java.math.BigDecimal

data class ProcessPaymentCommand(
    val orderId: Long,
    val amount: BigDecimal?
) {}
