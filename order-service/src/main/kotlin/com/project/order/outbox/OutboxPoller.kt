package com.project.order.outbox

import com.project.order.dictionary.OutboxStatus
import com.project.order.domain.OutboxEvent
import com.project.order.domain.dto.OrderCreatedEvent
import com.project.order.producer.KafkaNotificationProducer
import com.project.order.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant


//@Component
//class OutboxPoller(
//    val outboxEventRepository: OutboxEventRepository,
//    val kafkaNotificationProducer: KafkaNotificationProducer,
//    val objectMapper: ObjectMapper
//) {
//
//    private val log = LoggerFactory.getLogger(OutboxPoller::class.java)
//
//    @Scheduled(fixedDelay = 5000)
//    @Transactional
//    fun poll() {
//
//        //1. Забираем до 100 сбоытий со статусом NEW (самые старые первыми)
//        val events: List<OutboxEvent> = outboxEventRepository.claimNewEvents(100)
//
//        if (events.isEmpty()) {
//            return
//        }
//
//        log.info("Found ${events.size} events to publish")
//
//        //2. Для каждого события
//        for (event: OutboxEvent in events) {
//            try {
//                // 2.1 Десериализуем payload в OrderCreatedEvent
//                 val orderEvent: OrderCreatedEvent = event.payload
//
//                // 2.2 Восстанавливаем контекст трассировки (traceId, spanId) – важно для observability!
//                // Но пока просто отправим – observability вернёмся позже
//
//                // 2.3 Отправляем в Kafka
//                kafkaNotificationProducer.sendOrderCreatedEvent(orderEvent)
//
//                // 2.4 Обновляем статус и время отправки
//                event.status = OutboxStatus.PUBLISHED
//                event.processedAt = Instant.now()
//                outboxEventRepository.save(event)
//
//                log.info("Event for order {} published to Kafka", orderEvent.orderId);
//            } catch (e: Exception) {
//                log.error("Failed to publish event id={}", event.id, e);
////                // Статус не меняем – при следующем polling'е повторим
//            }
//        }
//
//
//    }
//}