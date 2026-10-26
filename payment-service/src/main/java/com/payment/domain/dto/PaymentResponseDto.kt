package com.payment.domain.dto

data class PaymentResponseDto(
    val isSuccessful : Boolean,
    val message: String,
    val requiresPendingProcessing: Boolean,
) {
}