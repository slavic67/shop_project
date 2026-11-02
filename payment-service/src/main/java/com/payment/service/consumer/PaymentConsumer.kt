package com.payment.service.consumer

import com.payment.domain.command.ProcessPaymentCommand
import com.payment.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.*
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


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
@KafkaListener(
    topics = ["\${app.topic.payment-commands}"], // берём из application.yaml
    groupId = "payment-service-group",
    containerFactory = "kafkaListenerContainerFactory"
)
class PaymentConsumer(
    val paymentService: PaymentService
) {

    private val log = LoggerFactory.getLogger(javaClass)


    @KafkaHandler
    fun handleOrderCreated(
        command: ProcessPaymentCommand,
        acknowledgment: Acknowledgment
    ) {
        log.info("Принято сообщение из топика payment-events");

        val orderId: Long = command.orderId
        log.info("Обработка оплаты для заказа $orderId")

        // 1. Выполняем оплату (имитация бизнес-логики)
        paymentService.processPayment(command)

        // 2. Подтверждаем offset
        acknowledgment.acknowledge()
    }

    @KafkaHandler(isDefault = true)
    fun handleUnknown(command: Any, acknowledgment: Acknowledgment) {
        log.warn("Неизвестный тип команды из payment-commands: {}", command.javaClass)
        acknowledgment.acknowledge()
    }

    @DltHandler
    fun handleDlt(command: Any?, acknowledgment: Acknowledgment) {
        log.error("Команда ушла в DLT после исчерпания попыток: {}", command)
        acknowledgment.acknowledge()
    }
}