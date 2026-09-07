package com.notification

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Thread.sleep

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun notify(@RequestBody request: NotificationRequest): Unit {
        log.info("Отправка сообщения по номеру заказа ${request.orderId}, тип ${request.eventType}")

        sleep(5000)

        return Unit
    }
}