package com.project.order.listener

import com.project.order.client.NotificationClient
import com.project.order.domain.dto.OrderCreatedEvent
import com.project.order.producer.KafkaNotificationProducer
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@RequiredArgsConstructor
@Deprecated("deprecated class")
class NotificationListener(
    private val kafkaNotificationProducer: KafkaNotificationProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {


        log.debug("Получено событие о создании заказа: {}, дата: {}", event.orderId, event.timestamp);

        try {
            kafkaNotificationProducer.sendOrderCreatedEvent(event);
            log.debug("Уведомление отправлено для заказа: {}", event.orderId);
        } catch (e: Exception) {
            log.error("Ошибка при отправке уведомления для заказа: {}", event.orderId, e);
            throw RuntimeException("Ошибка передачи данных в kafka")
        }

    }


}