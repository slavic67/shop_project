package com.project.order.outbox


import com.project.order.dictionary.OutboxStatus
import com.project.order.repository.OutboxEventRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import tools.jackson.databind.JsonNode
import java.util.concurrent.atomic.AtomicInteger


@Component
class DebeziumMetrics(
    val registry: MeterRegistry,
    val outboxRepository: OutboxEventRepository,

    @Value("\${debezium.connect.url}")
    val connectUrl: String,

    @Value("\${debezium.connector.name}")
    val connectorName: String

) {

    private val log = LoggerFactory.getLogger(DebeziumMetrics::class.java)
    private val restTemplate = RestTemplate()
    private final val connectorStatus: AtomicInteger = AtomicInteger(0)

    @PostConstruct
    fun void() {
        Gauge.builder(
            "outbox.events.pending",
            outboxRepository
        ) { repo -> repo.countByStatus(OutboxStatus.NEW).toDouble() }
            .description("Pending outbox events")
        .register(registry);

        Gauge.builder(
            "debezium.connector.status",
            connectorStatus
        ) { i -> i.get().toDouble() }
            .description("1=RUNNING, 0=FAILED")
            .register(registry);
    }

    @Scheduled(fixedDelay = 10000)
    fun refreshConnectorStatus() {

        try {

            val url =
                connectUrl +
                        "/connectors/" +
                        connectorName +
                        "/status"

            val response: JsonNode? =
                restTemplate.getForObject<JsonNode>(
                    url
                )

            val state =
                response!!.path("connector")
                    .path("state")
                    .asText()

            connectorStatus.set(
                if ("RUNNING" == state)
                    1
                else
                    0
            )
        } catch (e: Exception) {
            connectorStatus.set(0)

            log.warn("Cannot get Debezium status",e)
        }

    }


    fun incrementOutBoxCreated() {
        registry.counter("outbox.events.created.total").increment()
    }

    fun isConnectorRunning(): Boolean {
        return connectorStatus.get()==1
    }

}