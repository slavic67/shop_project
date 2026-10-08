package com.project.order.domain

import jakarta.persistence.*
import lombok.Getter
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
@Getter
class OrderItem(
    var productId: Long,
    var productName: String,
    var quantity: Int=0,
    var price: BigDecimal=BigDecimal.ZERO,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null


//    constructor(productId: Long?,
//                productName: String?,
//                quantity: Int,
//                price: BigDecimal
//    ) : this() {
//        this.productId = productId
//        this.productName = productName
//        this.quantity = quantity
//        this.price = price
//    }
}