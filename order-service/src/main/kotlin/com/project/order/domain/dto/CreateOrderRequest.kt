package com.project.order.domain.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.*

data class CreateOrderRequest(

    @field:Valid
    @field:NotEmpty(message = "items не должен быть пустым")
    val items: MutableList<OrderItemRequest>

)