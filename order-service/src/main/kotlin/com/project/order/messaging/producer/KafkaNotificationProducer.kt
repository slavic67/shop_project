package com.project.order.messaging.producer

import com.project.order.domain.event.OrderCreatedEvent
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@RequiredArgsConstructor
@Deprecated("deprecated class")
class KafkaNotificationProducer(
    @Value("\${app.topic.order-create-topic}")
    private val orderCreateTopic: String,

    // Используем надёжный продюсер (primary)
    private val reliableKafkaTemplate:  KafkaTemplate<String, Any>

    // Если хотим показать транзакционный, инжектим квалификатор
    // private final KafkaTemplate<String, Object> transactionalKafkaTemplate;
) {

    private val log = LoggerFactory.getLogger(KafkaNotificationProducer::class.java)


    fun sendOrderCreatedEvent(event: OrderCreatedEvent) {
        if (true) return

        //Все события для одного orderId будут попадать в одну партицию → сохраняется порядок событий по заказу
        val key: String = event.orderId.toString()


        //Возвращает CompletableFuture, который завершится, когда брокер подтвердит получение (или будет ошибка)
        val future: CompletableFuture<SendResult<String, Any>> =
            reliableKafkaTemplate.send(orderCreateTopic,key, event)


        //Асинхронное ожидание результата. Позволяет выполнить код после того, как брокер ответит (или ошибка), не блокируя основной поток
        future.whenComplete({ result: SendResult<String, Any>, ex: Throwable? ->
            if (ex == null) {
                log.info(
                    "Событие отправлено в topic: {}, partition: {}, offset: {}",
                    orderCreateTopic,
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset()
                )
            } else {
                log.error("Ошибка отправки события по orderId: {}", event.orderId, ex)
                // Здесь можно сохранить в outbox
            }
        })

    }
}