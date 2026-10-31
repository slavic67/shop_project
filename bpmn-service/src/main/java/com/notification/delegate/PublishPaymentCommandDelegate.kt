package com.notification.delegate

import com.notification.domain.command.ProcessPaymentCommand
import com.notification.messaging.producer.PaymentProducer
import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PublishPaymentCommandDelegate(private val paymentProducer: PaymentProducer) {

    private val log = LoggerFactory.getLogger(PublishPaymentCommandDelegate::class.java)


    @JobWorker(type = "publishPaymentCommand")
    fun execute(
        @Variable orderId: Long,
        @Variable totalAmount: BigDecimal?,
        @Variable traceparent: String
    ) {
//        execution!!

//        val orderId = execution.getVariable("orderId") as Long
//        val totalAmount = execution.getVariable("totalAmount") as BigDecimal
//        val traceparent = execution.getVariable("traceparent") as String

        log.info("Publishing ProcessPaymentCommand for order: {}", orderId)

        val command = ProcessPaymentCommand(
            orderId=orderId,
            amount = totalAmount,
        )

        try {
            paymentProducer.sendCommand(command, traceparent)
        } catch ( e: Exception) {
            log.error("Failed to send ProcessPaymentCommand for order: {}", orderId, e);

            //исключение, чтобы Camunda активировала ретраи
            throw RuntimeException("Failed to send ProcessPaymentCommand for order $orderId", e);
        }

    }
}