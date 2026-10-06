package com.notification

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private val log = LoggerFactory.getLogger(javaClass)
    private val random = Random(System.currentTimeMillis())
    private val failureMode: AtomicBoolean = AtomicBoolean(false)

    @PostMapping
    fun notify(@RequestBody request: NotificationRequest): Unit {

        if (failureMode.get()) {

            val randomInt = random.nextInt(100)

            log.info("Выпало число: $randomInt")

            if (randomInt <30) {
                log.error("Возникли проблемы с отправкой информации по заказу: ${request.orderId}")
                throw RuntimeException("Непредвиденная проблема при отправке уведомления")
            }

            if (randomInt > 70) {
                log.warn("NotificationService замедлился")
                Thread.sleep(200)
            }

            log.info("Сообщение по номеру заказа ${request.orderId}, типа: ${request.eventType}, успешно отправлено ")
        }



        return Unit
    }

    @GetMapping("/admin/failure-mode")
    fun setFailureMode(@RequestParam enabled: Boolean) {

        failureMode.set(enabled)

        log.info("Failure mode в Notification Service, переключен на: $enabled")
    }
}