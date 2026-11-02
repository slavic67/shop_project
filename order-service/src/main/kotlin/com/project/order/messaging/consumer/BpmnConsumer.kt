package com.project.order.messaging.consumer

import com.project.order.domain.command.CancelOrderCommand
import com.project.order.domain.command.UpdateOrderStatusCommand
import com.project.order.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.*
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper


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
    concurrency = "3",
)
@KafkaListener(
    topics = ["\${app.topic.order-commands}"],
    groupId = "order-service-group",
    containerFactory = "kafkaListenerContainerFactory"
)
class BpmnConsumer(
    private val objectMapper: ObjectMapper,
    private val orderService: OrderService
) {

    private val log = LoggerFactory.getLogger(BpmnConsumer::class.java)


    @KafkaHandler
    fun handleBpmCommand(
        updateCommand: UpdateOrderStatusCommand,
        acknowledgment: Acknowledgment
    ) {
        log.info(
            "Принято сообщение из топика order-commands UpdateOrderStatusCommand: {}",
            updateCommand
        )

        orderService.completeOrder(updateCommand)

        acknowledgment.acknowledge()
    }

    @KafkaHandler
    fun handleBpmCommand(
        failedCommand: CancelOrderCommand,
        acknowledgment: Acknowledgment
    ) {
        log.info("Принято сообщение из топика order-commands CancelOrderCommand: {}", failedCommand)

        orderService.cancelOrder(failedCommand)

        acknowledgment.acknowledge()
    }

    @KafkaHandler(isDefault = true)
    fun handleUnknown(command: Any, acknowledgment: Acknowledgment) {
        log.warn(
            "Неизвестный тип команды из order-commands: {}",
            command.javaClass
        )
        acknowledgment.acknowledge()
    }

    @DltHandler
    fun handleDlt(command: Any?, acknowledgment: Acknowledgment) {
        log.error(
            "Команда ушла в DLT после исчерпания попыток: {}",
            command
        )
        acknowledgment.acknowledge()
    }

}