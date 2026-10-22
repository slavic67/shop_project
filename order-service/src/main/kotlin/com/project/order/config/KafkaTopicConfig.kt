package com.project.order.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig (
    @Value("\${app.topic.order-create-topic}")
    private val orderCreateTopic: String
) {

    @Bean
    fun orderCreatedTopic(): NewTopic {
        return TopicBuilder.name(orderCreateTopic)
            .partitions(3) //для демонстрации ребаланса
            .replicas(1) //у нас один брокер
            .build()
    }
}