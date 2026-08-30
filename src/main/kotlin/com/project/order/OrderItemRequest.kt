package com.project.order

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class OrderItemRequest(

    @field:NotNull(message = "productId обязателен")
    val productId: Long,

    @field:NotBlank(message = "productName обязателен")
    val productName: String,

    @field:Min(value = 1, message = "Количество не может быть меньше 1")
    val quantity: Int,

    @field:NotNull(message = "price обязателен")
    @field:DecimalMin(value = "0.01", message = "price не может быть меньше 0")
    val price: BigDecimal
)