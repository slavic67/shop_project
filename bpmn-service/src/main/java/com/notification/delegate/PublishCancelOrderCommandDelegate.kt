package com.notification.delegate

import com.notification.domain.command.CancelOrderCommand
import com.notification.messaging.producer.OrderProducer
import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PublishCancelOrderCommandDelegate(
    private val orderProducer: OrderProducer
)  {

    private val log = LoggerFactory.getLogger(PublishCancelOrderCommandDelegate::class.java)

    @JobWorker(type="publishCancelOrderCommand")
    fun execute(
        @Variable orderId: Long,
        @Variable traceparent: String,
        @Variable("failureReason")  failureReason: String?
    ) {
        log.info("traceparent!!! {}", traceparent)

        // Если есть причина отмены – можно получить из переменной, иначе дефолтная
//        var reason = execution.getVariable("failureReason") as String?
        val reason = failureReason ?: "Payment timeout or failure"

        log.info("Publishing CancelOrderCommand for order: {} reason: {}", orderId, reason)

        val command = CancelOrderCommand(
            orderId = orderId,
            reason = reason,
        )

        try {
            orderProducer.sendCancelCommand(command, traceparent)
        } catch (e: Exception) {
            log.error("Failed to send CancelOrderCommand for order: {}", orderId, e)
            throw RuntimeException("Failed to send CancelOrderCommand for order $orderId", e)
        }

    }


}