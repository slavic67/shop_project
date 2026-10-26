package com.payment.domain.dto

import java.math.BigDecimal

data class PaymentRequestDto(
    val orderId: Long,
    val amount: BigDecimal
)
