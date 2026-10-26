package com.payment.service.consumer

import com.payment.domain.event.OrderCreatedEvent
import com.payment.service.PaymentService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.BackOff


@Component
class PaymentConsumer(
    val objectMapper: ObjectMapper,
    val paymentService: PaymentService
) {

    private val log = LoggerFactory.getLogger(javaClass)


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
        topics = ["\${app.topic.order-create-topic}"], // берём из application.yaml
        groupId = "payment-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderCreated(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment
    ) {
        log.info("Принято сообщение из топика order.outbox");

        try {
            // 1. Десериализуем событие
            val value: ByteArray = record.value()
            val event: OrderCreatedEvent = objectMapper.readValue(value, OrderCreatedEvent::class.java)
            val orderId: Long = event.orderId

            log.info("Обработка оплаты для заказа $orderId")

            // 3. Выполняем оплату (имитация бизнес-логики)
            paymentService.processPayment(event)

            // 4. Подтверждаем offset
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("Ошибка обработки события оплаты", e)
            // Не подтверждаем offset – после retry попадёт в DLT
            throw RuntimeException("Payment processing failed", e);
        }

    }

}