package com.project.order.domain

import com.project.order.OrderStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "orders")
class Order constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    var status: OrderStatus? =null

    @Column(name = "order_number", nullable = false, unique = true)
    var orderNumber: String?=null

    @Column(name = "created_at")
    var createAt: Instant? =null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    var items: MutableList<OrderItem> = mutableListOf()

    constructor(items: List<OrderItem>) : this() {
        this.status = OrderStatus.CREATED
        this.createAt = Instant.now()
        this.items.addAll(items)
        this.orderNumber = UUID.randomUUID().toString()

        items.forEach {item -> item.order=this}
    }
}