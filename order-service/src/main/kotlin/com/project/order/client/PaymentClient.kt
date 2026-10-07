package com.project.order.client

import com.project.order.domain.dto.PaymentRequestDto
import com.project.order.domain.dto.PaymentResponseDto
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.concurrent.Semaphore

@Component
@RequiredArgsConstructor
class PaymentClient(
    private val paymentWebClient: WebClient
) {

    private val semaphore: Semaphore = Semaphore(5)
    private val log = LoggerFactory.getLogger(PaymentClient::class.java)

    // нативная реализация bulkhead через semaphore
    fun processPaymentBulkhead(orderId: Long, amount: BigDecimal): PaymentResponseDto? {

        if (!semaphore.tryAcquire()) {
            log.warn("Bulkhead full")
            throw RuntimeException("Payment service busy")
        }

        try {
            return paymentWebClient.post()
                .bodyValue(PaymentRequestDto(orderId, amount))
                .retrieve()
                .bodyToMono<PaymentResponseDto>()
                .block()

        } finally {
            semaphore.release()
        }

    }


    fun processPayment(orderId: Long, amount: BigDecimal): Mono<PaymentResponseDto> {

        return paymentWebClient.post()
            .uri("/api/v1/payments/process")
            .bodyValue(PaymentRequestDto(orderId, amount))
            .retrieve()
            .bodyToMono<PaymentResponseDto>()

    }

}