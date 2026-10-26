package com.payment.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig (
    @Value("\${app.topic.payment-events}")
    private val paymentEventsTopic: String
) {

    @Bean
    fun orderCreatedTopic(): NewTopic {
        return TopicBuilder.name(paymentEventsTopic)
            .partitions(3) //для демонстрации ребаланса
            .replicas(1) //у нас один брокер
            .build()
    }
}