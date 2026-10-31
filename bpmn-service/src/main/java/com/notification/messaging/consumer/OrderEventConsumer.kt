package com.notification.messaging.consumer

import com.notification.domain.event.OrderCreatedEvent
import io.camunda.client.CamundaClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Header
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets


@Component
class OrderEventConsumer(
    private val client: CamundaClient,
//    private val runtimeService: RuntimeService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @KafkaListener(topics = ["\${app.topic.order-create-topic}"], groupId = "workflow-group")
    fun handleOrderCreated(record: ConsumerRecord<String, ByteArray>, acknowledgment: Acknowledgment) {
        try {

            val event: OrderCreatedEvent = objectMapper.readValue(record.value(), OrderCreatedEvent::class.java)

            log.info("Received OrderCreatedEvent for order: {}", event.orderId)

            // Запускаем BPMN-процесс
            val variables: MutableMap<String, Any> = HashMap()
            variables["orderId"] = event.orderId
            variables["timestamp"] = event.timestamp
            if (event.mdcContext!=null) variables["context"] = event.mdcContext
            variables["timeoutDuration"] = "PT30S"
            variables["traceparent"] = getTraceparent(record)!!


//
//            runtimeService.startProcessInstanceByKey("create-order-saga",variables)
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

    private fun getTraceparent(record: ConsumerRecord<String, ByteArray>): String? {

        var traceparent: String?=null
        val header: Header? = record.headers().lastHeader("trace_parent")
        if (header != null) {
            traceparent = String(header.value(), StandardCharsets.UTF_8)
        }

        log.info("Received trace parent for order: {}", traceparent);

        return traceparent
    }
}