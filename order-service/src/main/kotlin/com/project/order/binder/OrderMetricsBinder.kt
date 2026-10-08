package com.project.order.binder

import com.project.order.dictionary.OrderStatus
import com.project.order.repository.OrderRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder

import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@RequiredArgsConstructor
class OrderMetricsBinder(
    private val orderRepository: OrderRepository
): MeterBinder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun bindTo(registry: MeterRegistry) {

        //Лямбда вызывается при каждом скрейпе
        Gauge.builder("orders.pending.count", orderRepository
        ) { repo -> repo.countByStatus(OrderStatus.PENDING).toDouble() }
        .description("Current number of pending orders")
        .register(registry)

        //Для каждого статуса создаем Gaude с лямбдой
        for (status: OrderStatus in OrderStatus.entries) {
            Gauge.builder("orders.pending.status.count", orderRepository
            ) { repo -> repo.countByStatus(status).toDouble() }
            .tags("status", status.name)
            .description("Current number of orders with status $status")
            .register(registry)
        }

    }
}