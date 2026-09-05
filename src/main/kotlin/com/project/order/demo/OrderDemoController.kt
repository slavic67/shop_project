package com.project.order.demo

import com.project.order.CreateOrderRequest
import com.project.order.OrderResponse
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequiredArgsConstructor
@RequestMapping("/demo/orders")
class OrderDemoController(
    private val demoOrderService: DemoOrderService,
) {


    @PostMapping("/jdbc")
    fun createWithJdbc(@Valid @RequestBody request: CreateOrderRequest): OrderResponse {
        return demoOrderService.createWithJdbc(request)
    }

    @PostMapping("/jooq")
    fun createWithJooq(@Valid @RequestBody request: CreateOrderRequest): OrderResponse {
        return demoOrderService.createWithJooq(request)
    }


}