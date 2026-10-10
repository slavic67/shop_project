package com.project.order.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ProducerRabbitConfig {

    private val log = LoggerFactory.getLogger(ProducerRabbitConfig::class.java)

    companion object {
        const val EXCHANGE_NOTIFICATIONS: String = "notifications.exchange"
        const val ROUTING_KEY_NOTIFICATIONS: String = "notifications.queue"
    }

    @Bean
    fun notificationsExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NOTIFICATIONS, true, false)
    }

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter

        // Publisher Confirms — асинхронное подтверждение от брокера
        template.setConfirmCallback({
            correlationData, ack, cause ->
            if (!ack) {
                log.error("Message not confirmed. Correlation: ${correlationData?.id ?: "null"}, cause: $cause")
                //Здесь - сохранение в persistent storage для повтора
            } else {
                log.debug("Message confirmed. CorrelationId: ${correlationData?.id ?: "null"}")
            }

        })

        // Publisher Returns - когда сообщение не доставлено ни в одну очередь
        template.setReturnsCallback { returned ->
            log.error("Message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.exchange, returned.routingKey,
                returned.replyCode, returned.replyText);
        }

        template.setMandatory(true)  // без этого возвраты не работают
        template.isUsePublisherConnection = true // выделенное соединение для confirms

        return template

    }

}