package com.notification.delegate

import com.notification.domain.command.UpdateOrderStatusCommand
import com.notification.messaging.producer.OrderProducer
import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal


@Component
class PublishUpdateOrderStatusCommandDelegate(
    private val orderProducer: OrderProducer
) {

    private val log = LoggerFactory.getLogger(PublishUpdateOrderStatusCommandDelegate::class.java)

    @JobWorker(type = "publishUpdateOrderStatusCommand")
    fun execute(
        @Variable orderId: Long,
        @Variable transactionId: String,
        @Variable traceparent: String
    ) {
//        execution!!
//
//        val orderId = execution.getVariable("orderId") as Long
//        val transactionId = execution.getVariable("transactionId") as String
//        val traceparent = execution.getVariable("traceparent") as String

        log.info("traceparent!!! {}", traceparent)

        log.info("Publishing UpdateOrderStatusCommand (PAID) for order: {}", orderId)

        val command: UpdateOrderStatusCommand = UpdateOrderStatusCommand(
            orderId=orderId,
            transactionId=transactionId,
            newStatus = "PAID"
        )

        try {
            orderProducer.sendUpdateCommand(command,  traceparent);
        } catch(e: Exception) {
            log.error("Failed to send UpdateOrderStatusCommand for order: {}", orderId, e);
            throw RuntimeException("Failed to send UpdateOrderStatusCommand for order $orderId", e);
        }

    }
}