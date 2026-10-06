package com.project.order.client

import com.project.order.domain.dto.PaymentRequestDto
import com.project.order.domain.dto.PaymentResponseDto
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Component
@RequiredArgsConstructor
class PaymentClient(
    private val paymentWebClient: WebClient
) {

    fun processPayment(orderId: Long, amount: BigDecimal): Mono<PaymentResponseDto> {

        return paymentWebClient.post()
            .uri("/api/v1/payments/process")
            .bodyValue(PaymentRequestDto(orderId, amount))
            .retrieve()
            .bodyToMono<PaymentResponseDto>()

    }

}