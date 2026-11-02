package com.notification.messaging.consumer

import com.notification.domain.event.PaymentCompletedEvent
import com.notification.domain.event.PaymentFailedEvent
import io.camunda.client.CamundaClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
@RetryableTopic(
    attempts = "3",
    backOff = BackOff(delay = 1000, maxDelay = 10000, multiplier = 2.0),
    timeout = "60000",
    retryTopicSuffix = "-retry",
    dltTopicSuffix = ".DLT",
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
    exclude = [IllegalArgumentException::class, NullPointerException::class],
    traversingCauses = "true",
    dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
    autoCreateTopics = "true",
    numPartitions = "1",
    replicationFactor = "1",
    listenerContainerFactory = "kafkaListenerContainerFactory",
    kafkaTemplate = "reliableKafkaTemplate",
    concurrency = "3"
)
@KafkaListener(topics = ["\${app.topic.payment-events}"], groupId = "workflow-group")
class PaymentEventConsumer(
    private val client: CamundaClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @KafkaHandler
    private fun handlePaymentCompleted(
        event: PaymentCompletedEvent,
        acknowledgment: Acknowledgment
    ) {
        log.info("Received PaymentCompletedEvent for order: {}", event.orderId)

        client.newPublishMessageCommand()
            .messageName("paymentCompleted")
            .correlationKey(event.orderId.toString())
            .variables(mapOf<String, Any>("transactionId" to event.transactionId))
            .timeToLive(Duration.ofMinutes(5))
            .send()
            .join()

        acknowledgment.acknowledge()
    }

    @KafkaHandler
    private fun handlePaymentFailed(
        event: PaymentFailedEvent,
        acknowledgment: Acknowledgment
    ) {
        log.info("Received PaymentFailedEvent for order: {}", event.orderId)

        client.newPublishMessageCommand()
            .messageName("paymentFailed")
            .correlationKey(event.orderId.toString())
            .variables(mapOf<String, Any>("failureReason" to event.reason))
            .timeToLive(Duration.ofMinutes(5))
            .send()
            .join()

        acknowledgment.acknowledge()
    }

    @KafkaHandler(isDefault = true)
    fun handleUnknown(event: Any, acknowledgment: Acknowledgment) {
        log.warn("Неизвестный тип payment-события: {}", event.javaClass);
        acknowledgment.acknowledge();
    }
}