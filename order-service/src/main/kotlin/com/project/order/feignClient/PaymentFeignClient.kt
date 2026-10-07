package com.project.order.feignClient

import com.project.order.config.FeignConfig
import com.project.order.domain.dto.PaymentRequestDto
import com.project.order.domain.dto.PaymentResponseDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping

@FeignClient(
    name = "payment-service",
    url = $$"${payment-service.url}",
    configuration = [FeignConfig::class]
)
interface PaymentFeignClient {

    @PostMapping("/api/v1/payments/process")
    fun processPayment(paymentRequestDto: PaymentRequestDto): PaymentResponseDto?

}