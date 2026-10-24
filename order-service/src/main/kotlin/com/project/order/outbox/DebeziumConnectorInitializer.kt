package com.project.order.outbox

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component


@Component
class DebeziumConnectorInitializer(
    val kafkaConnectService: KafkaConnectService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        kafkaConnectService.createOutboxConnectorIfNotExists()
    }
}