package com.notification.messaging.consumer

import com.notification.domain.event.PaymentCompletedEvent
import com.notification.domain.event.PaymentFailedEvent
import io.camunda.client.CamundaClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class PaymentEventConsumer(
    private val client: CamundaClient,
//    private val runtimeService: RuntimeService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics= ["\${app.topic.payment-events}"], groupId = "workflow-group")
    fun handlePaymentEvent(record: ConsumerRecord<String, ByteArray>, acknowledgment: Acknowledgment) {
        try {

            val json: String = record.value().toString(Charsets.UTF_8)
            log.debug("Received payment event: {}", json)

            // Определяем тип события по наличию поля transactionId
            if (json.contains("\"transactionId\"")) {
                handlePaymentCompleted(json)
            } else {
                handlePaymentFailed(json)
            }

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            log.error("Failed to process payment event",e)
            // При ошибке не коммитим offset – сообщение будет обработано повторно
        }
    }

    private fun handlePaymentCompleted(json: String) {
        val event: PaymentCompletedEvent =
            objectMapper.readValue(json, PaymentCompletedEvent::class.java)
        log.info("Received PaymentCompletedEvent for order: {}", event.orderId)


        // Коррелируем сообщение с процессом
//        runtimeService.createMessageCorrelation("paymentCompleted")
//            .processInstanceVariableEquals("orderId", event.orderId)
//            .setVariable("transactionId", event.transactionId)
//            .correlateWithResult()

        client.newPublishMessageCommand()
            .messageName("paymentCompleted")
            .correlationKey(event.orderId.toString())
            .variables(mapOf<String, Any>("transactionId" to event.transactionId))
            .timeToLive(Duration.ofMinutes(5))
            .send()
            .join()
    }

    private fun handlePaymentFailed(json: String) {
        val event: PaymentFailedEvent =
            objectMapper.readValue(json, PaymentFailedEvent::class.java)
        log.info("Received PaymentFailedEvent for order: {}", event.orderId)


        // Коррелируем сообщение с процессом
//        runtimeService.createMessageCorrelation("paymentFailed")
//            .processInstanceVariableEquals("orderId", event.orderId)
//            .setVariable("failureReason", event.reason)
//            .correlateWithResult()

        client.newPublishMessageCommand()
            .messageName("paymentFailed")
            .correlationKey(event.orderId.toString())
            .variables(mapOf<String, Any>("failureReason" to event.reason))
            .timeToLive(Duration.ofMinutes(5))
            .send()
            .join()
    }
}