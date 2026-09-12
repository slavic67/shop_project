package com.notification

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun notify(@RequestBody request: NotificationRequest): Unit {
        log.info("Отправка сообщения по номеру заказа ${request.orderId}, тип ${request.eventType}")

        //TODO имитация проблемы, потом удалить
        val random: Int = Random.nextInt(100)

        log.info("Выпало число: $random")

        if (random < 10) {
            log.error("Возникли проблемы с отправкой уведомления по orderId: ${request.orderId}")
            throw RuntimeException("Ошибка отправки сообщения")
        }

        if (random > 90) {
            log.info("Сервис замедлился")
            Thread.sleep(600)
        }

        return Unit
    }
}