package com.project.order.outbox

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component


@Component
class DebeziumHealthIndicator(
    val metrics: DebeziumMetrics
) : HealthIndicator {

    override fun health(): Health? {
        return if (metrics.isConnectorRunning())
            Health.up().build()
        else
            Health.down().build()
    }
}