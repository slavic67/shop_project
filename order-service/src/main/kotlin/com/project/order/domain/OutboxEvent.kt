package com.project.order.domain

import com.project.order.dictionary.OutboxStatus
import com.project.order.domain.dto.OrderCreatedEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant


@Entity
@Table(name = "outbox_event")
class OutboxEvent(

    @Column(name = "aggregate_type", nullable = false)
    var aggregateType: String,

    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: String,

    @Column(name = "event_type", nullable = false)
    var eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    var payload: OrderCreatedEvent,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus,

    @Column(name = "trace_id", nullable = false)
    var  traceId: String,

    @Column(name = "span_id", nullable = false)
    var  spanId: String
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     var id: Long? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false)
     var  createdAt: Instant = Instant.now()

    @Column(name = "processed_at")
    @LastModifiedDate
     var processedAt: Instant = Instant.now()
}