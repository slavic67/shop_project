package com.notification.messaging.consumer

import com.notification.domain.event.OrderCreatedEvent
import io.camunda.client.CamundaClient
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper


@Component
class OrderEventConsumer(
    private val client: CamundaClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @KafkaListener(topics = ["\${app.topic.order-create-topic}"], groupId = "workflow-group")
    fun handleOrderCreated(
        event: OrderCreatedEvent,
        @Header(name="traceparent", required=false) traceparent: String,
        acknowledgment: Acknowledgment
    ) {
        try {

            log.info("Received payment event: {}", event);

            log.info("Received OrderCreatedEvent for order: {}", event.orderId)

            // Запускаем BPMN-процесс
            val variables: MutableMap<String, Any> = HashMap()
            variables["orderId"] = event.orderId
            variables["timestamp"] = event.timestamp
            if (event.mdcContext != null) variables["context"] = event.mdcContext
            variables["timeoutDuration"] = "PT30S"
            variables["traceparent"] = traceparent

            client.newCreateInstanceCommand()
                .bpmnProcessId("create-order-saga")
                .latestVersion()
                .variables(variables)
                .send()
                .join()
                .processInstanceKey


            log.info("BPMN process started for order: {}", event.orderId)
            acknowledgment.acknowledge()

        } catch (e: Exception) {
            log.error("Failed to start workflow for order", e)
        }
    }
}