package com.project.order.domain

import com.project.order.dictionary.OrderStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "orders")
class Order(
    @Enumerated(EnumType.STRING)
    var status: OrderStatus= OrderStatus.CREATED,

    @Column(name = "order_number", nullable = false, unique = true)
    var orderNumber: String=UUID.randomUUID().toString(),

    @Column(name = "created_at")
    var createAt: Instant = Instant.now()
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null



    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    var items: MutableList<OrderItem> = mutableListOf()

    constructor(items: List<OrderItem>) : this(
        status = OrderStatus.CREATED,
        orderNumber = UUID.randomUUID().toString(),
        createAt = Instant.now()
    ) {
        this.items.addAll(items)
        items.forEach {item -> item.order=this}
    }

    fun addItems(items: List<OrderItem>) {
        this.items.addAll(items)
        items.forEach {item -> item.order=this}
    }


    //метод расчета суммы заказа
    fun calculateTotalAmount() : BigDecimal {
        if (this.items.isEmpty()) {
            return BigDecimal.ZERO
        }

        return items.stream().map { item ->
            item.price.multiply(BigDecimal.valueOf(item.quantity.toLong()))
        }.reduce(BigDecimal.ZERO, BigDecimal::add)
    }

    fun getTotalPrice() : BigDecimal {
        if (this.items.isEmpty()) {
            return BigDecimal.ZERO
        }

        return items.stream().map(OrderItem::price).reduce(BigDecimal.ZERO, BigDecimal::add)
    }

}