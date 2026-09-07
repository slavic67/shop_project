package com.project.order.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class WebClientConfig {

    @Value("\${url.notification-service}")
    private val url: String? = null


    @Bean
    fun notificationWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(url!!).build()
    }
}