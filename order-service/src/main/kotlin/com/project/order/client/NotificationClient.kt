package com.project.order.client

import com.project.order.domain.dto.NotificationRequest
import com.project.order.domain.dto.OrderCreatedEvent
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
@RequiredArgsConstructor
class NotificationClient(
    private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun notifyOrderCreated(event: OrderCreatedEvent) {

        val mdcContext: Map<String, String>? = event.mdcContext

        if (mdcContext != null) {
            MDC.setContextMap(mdcContext)
        }

        val orderId: Long = event.orderId

        try {

            log.info("Отправка уведомления для заказа $orderId с traceId: ${MDC.get("traceId")}, сумма: ${MDC.get("total_amount")}")

            val request: NotificationRequest = NotificationRequest(orderId, "CREATED")


            webClient.post()
                .uri("/api/notifications")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block()
        } finally {
            MDC.clear() // очищаем в асинхронном потоке
        }


    }
}