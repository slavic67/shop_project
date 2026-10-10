package com.project.order.producer

import com.project.order.config.ProducerRabbitConfig
import com.project.order.domain.dto.OrderCreatedEvent
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID


@Service
@RequiredArgsConstructor
class NotificationProducer(
    private val rabbitTemplate: RabbitTemplate
) {

    private val log = LoggerFactory.getLogger(NotificationProducer::class.java)

    fun sendOrderCreatedEvent(event: OrderCreatedEvent) {

        // 1. Создаём корреляционные данные — уникальный идентификатор этого сообщения
        val correlationData: CorrelationData = CorrelationData(UUID.randomUUID().toString())

        // 2. Логируем факт отправки, чтобы можно было сопоставить с confirm/return
        log.info("Sending OrderCreatedEvent: orderId={}, correlationId={}", event.orderId, correlationData.id)

        // 3. Отправляем ссобщение в exchange с routing key и корреляцией
        rabbitTemplate.convertAndSend(
            ProducerRabbitConfig.EXCHANGE_NOTIFICATIONS,   // exchange
            ProducerRabbitConfig.ROUTING_KEY_NOTIFICATIONS, // routing key
            event,                                          // payload
            correlationData                                 // для confirms
        )
    }

}