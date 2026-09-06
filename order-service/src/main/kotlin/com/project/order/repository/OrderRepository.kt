package com.project.order.repository

import com.project.order.Order
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OrderRepository : JpaRepository<Order, Long> {

    /**
     * Загрузка заказа вместе с его позициями
     * Решает классическую проблему N+1 запросов в JPA/Hibernate
     *
     * Без @EntityGraph:
     * 1. SELECT * FROM orders WHERE id = ?  ← 1 запрос за заказом
     * 2. SELECT * FROM order_items WHERE order_id = ?  ← N запросов за позициями
     *
     * С @EntityGraph:
     * 1. SELECT o.*, i.* FROM orders o
     * LEFT JOIN order_items i ON o.id = i.order_id
     * WHERE o.id = ?  ← ТОЛЬКО 1 запрос
     */
    @EntityGraph(attributePaths = ["items"])
    fun findWithItemsById(id: Long): Optional<Order>
}