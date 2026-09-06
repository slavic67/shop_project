package com.project.order

import jakarta.persistence.*
import lombok.AccessLevel
import lombok.Getter
import lombok.NoArgsConstructor
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
@Getter
class OrderItem() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var productId: Long?=null
    var productName: String?=null
    var quantity: Int=0
    var price: BigDecimal?=null

    constructor(productId: Long?,
                productName: String?,
                quantity: Int,
                price: BigDecimal?
    ) : this() {
        this.productId = productId
        this.productName = productName
        this.quantity = quantity
        this.price = price
    }
}