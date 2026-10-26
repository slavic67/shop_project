package com.payment.service.producer

import com.payment.domain.event.PaymentCompletedEvent
import com.payment.domain.event.PaymentFailedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class PaymentProducer(

    @Value("\${app.topic.payment-events}")
    private val paymentEventsTopic: String,

    // Используем надёжный продюсер (primary)
    private val reliableKafkaTemplate: KafkaTemplate<String, Any>
) {

    private val log = LoggerFactory.getLogger(javaClass)


    fun sendPaymentCompletedEvent(event: PaymentCompletedEvent) {
        // Все события для одного orderId будут попадать в одну партицию -> сохраняется порядок событий по заказу
        send(paymentEventsTopic, event.orderId.toString(),event)
    }

    fun sendPaymentFailedEvent(event: PaymentFailedEvent) {
        send(paymentEventsTopic, event.orderId.toString(), event)
    }


    private fun send(topic: String, key: String, event: Any) {

        //Возвращает CompletableFuture, который завершится, когда брокер подтвердит получение (или будет ошибка)
        val future: CompletableFuture<SendResult<String, Any>> = reliableKafkaTemplate.send(topic, key, event)

        //Асинхронное ожидание результата. Позволяет выполнить код после того, как брокер ответит (или ошибка), не блокируя основной поток
        future.whenComplete { result, ex ->
            if (ex==null) {
                log.info("Событие отправлено в topic: {}, partition: {}, offset: {}",
                    paymentEventsTopic,
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset()
                );
            } else {
                log.error("Ошибка отправки события по orderId: {}", key, ex)
                // Здесь можно сохранить в outbox
            }
        }

    }
}