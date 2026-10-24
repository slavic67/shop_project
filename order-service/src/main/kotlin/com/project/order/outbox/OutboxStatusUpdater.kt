package com.project.order.outbox

import com.project.order.dictionary.OutboxStatus
import com.project.order.repository.OutboxEventRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class OutboxStatusUpdater(
    val outboxRepository: OutboxEventRepository,
    val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(OutboxStatusUpdater::class.java)

    @KafkaListener(
        topics = ["\${debezium.connector.outbox-route-topic}"],
        groupId = "order-service-status-updater",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun updateStatus(record: ConsumerRecord<String, String>) {
        try {
            // Из payload достаем id события
            val json: JsonNode = objectMapper.readTree(record.value())
            val eventId : Long = json.get("orderId").asLong()

            // Обновляем статус или удаляем запись
            outboxRepository.updateStatus(eventId, OutboxStatus.PUBLISHED);

            log.info("Обновлен outbox статус события {} на PUBLISHED", eventId);

        } catch (e: Exception) {
            log.error("Failed to update outbox status", e)
        }
    }
}