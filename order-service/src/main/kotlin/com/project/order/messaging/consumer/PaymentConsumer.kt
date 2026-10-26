package com.project.order.messaging.consumer

import com.project.order.domain.event.PaymentCompletedEvent
import com.project.order.domain.event.PaymentFailedEvent
import com.project.order.service.OrderService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentConsumer(
    private val objectMapper: ObjectMapper,
    private val orderService: OrderService
) {

    private val log = LoggerFactory.getLogger(PaymentConsumer::class.java)


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
        concurrency = "3",
        autoStartDltHandler = "true"
    )
    @KafkaListener(
        topics = ["\${app.topic.payment-events}"],
        groupId = "order-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handlePaymentEvent(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment
    ) {
        log.info("Принято сообщение из топика payment-service")

        try {
            val value: ByteArray = record.value()
            // Определяем тип события по заголовку или по содержимому
            // Для простоты будем пробовать десериализовать сначала как PaymentCompletedEvent,
            // если не получится – пробуем как PaymentFailedEvent.
            // Но лучше использовать заголовок __TypeId__, который добавляет JsonSerializer.
            // В нашем случае мы можем положить тип в отдельное поле, или использовать заголовки.
            // Для демонстрации я покажу упрощённый вариант:

            val json: String = String(value)

            if (json.contains("\"transactionId\"")) {

                val completed: PaymentCompletedEvent = objectMapper.readValue(value, PaymentCompletedEvent::class.java)
                orderService.completeOrder(completed.orderId)
            } else if (json.contains("\"reason\"")) {

                val failed: PaymentFailedEvent = objectMapper.readValue(value, PaymentFailedEvent::class.java)
                orderService.cancelOrder(failed.orderId, failed.reason)
            } else {
                throw IllegalArgumentException("Unknown event type")
            }

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            log.error("шибка обработки события оплаты",e)
            throw RuntimeException("Payment event processing failed", e)
        }
    }
}