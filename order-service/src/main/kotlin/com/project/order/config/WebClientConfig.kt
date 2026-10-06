package com.project.order.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class WebClientConfig {
    @Value("\${url.notification-service}")
    private var notificationUrl: String?=null

    @Value("\${url.payment-service}")
    private var paymentUrl: String?=null

    @Bean
    fun notificationWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(notificationUrl!!).build()
    }

    @Bean
    fun paymentWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(paymentUrl!!).build()
    }
}