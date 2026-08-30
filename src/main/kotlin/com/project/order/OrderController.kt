package com.project.order

import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
class OrderController (
    val orderService: OrderService
){

    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ) : OrderResponse? {
        val order: OrderResponse? = orderService.createOrder(request)
        return order
    }
}