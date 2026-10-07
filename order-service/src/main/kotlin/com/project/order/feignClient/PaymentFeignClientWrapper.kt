package com.project.order.feignClient

import com.project.order.config.FeignConfig
import com.project.order.domain.dto.PaymentRequestDto
import com.project.order.domain.dto.PaymentResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

// Нативное ограничение количества занятых потоков (Bulkhead для FeignClient)
@Component
class PaymentFeignClientWrapper(
    @Value($$"${bulkhead.countThreads:5}") countThreads: Int,
    private val client : PaymentFeignClient,
    private val semaphore: Semaphore = Semaphore(countThreads)
) {

    private val log = LoggerFactory.getLogger(PaymentFeignClientWrapper::class.java)

    fun processPayment(request: PaymentRequestDto): PaymentResponseDto? {
        if (!semaphore.tryAcquire()) {
            log.warn("Bulkhead full, rejecting call")
            throw RuntimeException("Service busy")
        }
        try {
            return client.processPayment(request)
        } finally {
            semaphore.release()
        }
    }

}