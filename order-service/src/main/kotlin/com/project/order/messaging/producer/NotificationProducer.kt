package com.project.order.messaging.producer

import com.project.order.domain.event.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer


@Service
class NotificationProducer(

    @Value("\${app.topic.notification-events}")
    val notificationEventsTopic: String,

    private val reliableKafkaTemplate: KafkaTemplate<String, Any>
) {

    private val log = LoggerFactory.getLogger(NotificationProducer::class.java)

    fun sendOrderUpdatedEvent(event: NotificationEvent) {

        val key: String = event.orderId.toString()

        val future: CompletableFuture<SendResult<String, Any>> = reliableKafkaTemplate.send(
            notificationEventsTopic,
            key,
            event
        )

        future.whenComplete { result, ex ->
            if (ex == null) {
                log.info(
                    "Событие отправлено в topic: {}, partition: {}, offset: {}",
                    notificationEventsTopic,
                    result!!.recordMetadata.partition(),
                    result.recordMetadata.offset()
                )
            } else {
                log.error("Ошибка отправки события по orderId: {}", event.orderId, ex)
                // Здесь можно сохранить в outbox
            }
        }

    }
}