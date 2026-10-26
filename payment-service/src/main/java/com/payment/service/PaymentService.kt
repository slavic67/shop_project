package com.payment.service

import com.payment.domain.event.OrderCreatedEvent
import com.payment.domain.event.PaymentCompletedEvent
import com.payment.domain.event.PaymentFailedEvent
import com.payment.service.producer.PaymentProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicBoolean


@Service
class PaymentService(
    private val paymentProducer: PaymentProducer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val failureMode = AtomicBoolean(true)


    /**
     * Имитация бизнес-логики оплаты.
     * Здесь может быть вызов внешнего платёжного шлюза, работа с БД и т.д.
     * Для демонстрации используем существующий PaymentController, но можно просто заглушку.
     */
    @Transactional
    fun processPayment(event: OrderCreatedEvent) {

        //тут логика оплаты которой нет

        // Отправляем результат
        if (failureMode.get()) {
            paymentProducer.sendPaymentCompletedEvent(
                PaymentCompletedEvent(
                    orderId = event.orderId,
                    transactionId = "txn_" + System.currentTimeMillis(),
                    "COMPLETED"
                )
            )

            log.info("Оплата успешна для заказа {}, отправлено PaymentCompletedEvent", event.orderId);
        } else {
            paymentProducer.sendPaymentFailedEvent(
                PaymentFailedEvent(
                    event.orderId,
                    "Внутренняя ошибка сервиса"
                )
            )
            log.warn("Оплата не удалась для заказа {}, отправлено PaymentFailedEvent", event.orderId);
        }

    }


    fun setFailureMode(enabled: Boolean) {
        failureMode.set(enabled)

        log.info("Failure mode в Payment Service, переключен на: {}", enabled);
    }

}