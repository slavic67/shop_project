package com.notification.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig (
//    @Value("\${app.topic.order-events}")
//    private var orderEventsTopic: String,

    @Value("\${app.topic.payment-events}")
    private var paymentEventsTopic: String,

    @Value("\${app.topic.payment-commands}")
    private var paymentCommandsTopic: String,

    @Value("\${app.topic.order-commands}")
    private var orderCommandsTopic: String
) {

//    @Bean
//    fun orderEventsTopic(): NewTopic {
//        return TopicBuilder.name(orderEventsTopic)
//            .partitions(3)
//            .replicas(1)
//            .build()
//    }

    @Bean
    fun paymentEventsTopic(): NewTopic {
        return TopicBuilder.name(paymentEventsTopic)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun paymentCommandsTopic(): NewTopic {
        return TopicBuilder.name(paymentCommandsTopic)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun orderCommandsTopic(): NewTopic {
        return TopicBuilder.name(orderCommandsTopic)
            .partitions(3)
            .replicas(1)
            .build()
    }
}