package com.project.order

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

data class CreateOrderRequest(

    @field:Valid
    @field:NotEmpty(message = "items не должен быть пустым")
    val items: MutableList<OrderItemRequest>

)