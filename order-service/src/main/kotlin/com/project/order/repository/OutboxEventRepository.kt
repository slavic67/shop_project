package com.project.order.repository

import com.project.order.dictionary.OutboxStatus
import com.project.order.domain.event.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {


    //    //несколько poller'ов могут забрать одни и те же события (дубликаты в Kafka) - решение FOR UPDATE SKIP LOCKED
//    @Modifying
//    @Query(value = """
//    UPDATE outbox_event
//    SET status = 'PROCESSING', processed_at = now()
//    WHERE id IN (
//        SELECT id FROM outbox_event
//        WHERE status = 'NEW'
//        ORDER BY created_at
//        LIMIT :limit
//        FOR UPDATE SKIP LOCKED
//    )
//    RETURNING *
//    """, nativeQuery = true)
//    fun claimNewEvents(@Param("limit") limit: Int): List<OutboxEvent>;

    @Modifying
    @Query("""
        update OutboxEvent o
        set o.status = :status, o.processedAt = CURRENT_TIMESTAMP 
        where o.aggregateId = :eventId 
        and o.status = 'NEW'
    """)
    fun updateStatus(
        @Param("eventId") eventId: Long,
        @Param("status") status: OutboxStatus
    )

    fun countByStatus(status: OutboxStatus): Int

    override fun count(): Long
}