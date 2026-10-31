package com.payment.domain.command

import jdk.jfr.DataAmount
import java.math.BigDecimal

data class ProcessPaymentCommand(
    val orderId: Long,
    val amount: BigDecimal?
)
