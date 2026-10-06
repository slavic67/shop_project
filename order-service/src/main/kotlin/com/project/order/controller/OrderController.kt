package com.project.order.controller

import com.project.order.service.OrderService
import com.project.order.domain.dto.CreateOrderRequest
import com.project.order.domain.dto.OrderResponse
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
class OrderController (
    val orderService: OrderService
){

    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ) : OrderResponse? {
        val order: OrderResponse? = orderService.createOrder(request)
        return order
    }

    @GetMapping("/{id}")
    fun getOrderWithItems(@PathVariable id: Long): OrderResponse? {
        return orderService.getOrderWithItems(id)
    }

    @GetMapping("/admin/failure-mode")
    fun setFailureMode(@RequestParam enabled: Boolean){
        orderService.setFailureMode(enabled)
    }
}