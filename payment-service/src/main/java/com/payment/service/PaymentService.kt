package com.payment.service

import com.payment.domain.command.ProcessPaymentCommand
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
    fun processPayment(command: ProcessPaymentCommand) {

        //тут логика оплаты которой нет
        log.info("Processing Payment Command: {}", command);

        // Отправляем результат
        if (failureMode.get()) {
            paymentProducer.sendPaymentCompletedEvent(
                PaymentCompletedEvent(
                    orderId = command.orderId,
                    transactionId = "txn_" + System.currentTimeMillis(),
                    "COMPLETED"
                )
            )

            log.info("Оплата успешна для заказа {}, отправлено PaymentCompletedEvent", command.orderId);
        } else {
            paymentProducer.sendPaymentFailedEvent(
                PaymentFailedEvent(
                    command.orderId,
                    "Внутренняя ошибка сервиса"
                )
            )
            log.warn("Оплата не удалась для заказа {}, отправлено PaymentFailedEvent", command.orderId);
        }

    }


    fun setFailureMode(enabled: Boolean) {
        failureMode.set(enabled)

        log.info("Failure mode в Payment Service, переключен на: {}", enabled);
    }

}