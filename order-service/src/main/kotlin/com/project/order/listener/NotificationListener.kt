package com.project.order.listener

import com.project.order.domain.event.NotificationEvent
import com.project.order.messaging.producer.NotificationProducer
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@RequiredArgsConstructor
class NotificationListener(
    private val kafkaNotificationProducer: NotificationProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderUpdated(event: NotificationEvent) {


        log.debug("Получено событие о результате заказа: {}, результат-статус: {}", event.orderId, event.status);

        try {
            kafkaNotificationProducer.sendOrderUpdatedEvent(event);
            log.debug("Уведомление отправлено для заказа: {}", event.orderId);
        } catch (e: Exception) {
            log.error("Ошибка при отправке уведомления для заказа: {}", event.orderId, e);
            throw RuntimeException("Ошибка передачи данных в kafka")
        }

    }


}