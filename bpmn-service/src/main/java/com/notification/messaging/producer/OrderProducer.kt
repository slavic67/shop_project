package com.notification.messaging.producer

import com.notification.domain.command.CancelOrderCommand
import com.notification.domain.command.UpdateOrderStatusCommand
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture


@Service
class OrderProducer(

    @Value("\${app.topic.order-commands}")
    private val orderCommandsTopic: String,

    private val reliableKafkaTemplate: KafkaTemplate<String,Any>
) {

    private val log = LoggerFactory.getLogger(OrderProducer::class.java)

    fun sendUpdateCommand(command: UpdateOrderStatusCommand, traceparent: String) {
        send(orderCommandsTopic, command.orderId.toString(), command, traceparent)
    }

    fun sendCancelCommand(command: CancelOrderCommand, traceparent: String) {
        send(orderCommandsTopic, command.orderId.toString(), command, traceparent)
    }


    private fun send(topic: String, key: String, event: Any, traceparent: String?) {
        val record: ProducerRecord<String, Any> = ProducerRecord(topic, key, event)

        if (traceparent!=null) {
            record.headers().add("traceparent", traceparent
                .toByteArray(StandardCharsets.UTF_8))
        }

        //Возвращает CompletableFuture, который завершится, когда брокер подтвердит получение (или будет ошибка)
        val future: CompletableFuture<SendResult<String, Any>> = reliableKafkaTemplate.send(record)

        //Асинхронное ожидание результата. Позволяет выполнить код после того, как брокер ответит (или ошибка), не блокируя основной поток
        future.whenComplete { result, ex ->
            if (ex==null) {
                log.info("Событие отправлено в topic: {}, partition: {}, offset: {}",
                    orderCommandsTopic,
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset()
                );
            } else {
                log.error("Ошибка отправки события по orderId: {}", key, ex);
                // Здесь можно сохранить в outbox
            }
        }
    }

}
