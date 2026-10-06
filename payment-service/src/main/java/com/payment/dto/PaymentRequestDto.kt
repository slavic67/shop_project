package com.payment.dto

import java.math.BigDecimal

data class PaymentRequestDto(
    val orderId: Long,
    val amount: BigDecimal
)
