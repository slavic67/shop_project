package com.project.order

import com.project.order.client.NotificationClient
import com.project.order.domain.dto.OrderCreatedEvent
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@RequiredArgsConstructor
class NotificationListener(
    private val notificationClient: NotificationClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {


        log.debug("Получено событие о создании заказа: {}, дата: {}", event.orderId, event.timestamp);

        try {
            notificationClient.notifyOrderCreated(event);
            log.debug("Уведомление отправлено для заказа: {}", event.orderId);
        } catch (e: Exception) {
            log.error("Ошибка при отправке уведомления для заказа: {}", event.orderId, e);
        }

    }


}