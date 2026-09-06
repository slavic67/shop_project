package com.project.order.client

import com.project.order.domain.dto.NotificationRequest
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
@RequiredArgsConstructor
class NotificationClient(
    private val webClient: WebClient
) {

    fun notifyOrderCreated(orderId: Long) {

        val request: NotificationRequest = NotificationRequest(orderId, "CREATED")

        webClient.post()
            .uri("/api/notifications")
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}