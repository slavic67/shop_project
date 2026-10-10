package com.payment.controller

import com.payment.dto.PaymentRequestDto
import com.payment.dto.PaymentResponseDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random


@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {

    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    private val failureMode: AtomicBoolean = AtomicBoolean(false)
    private val random = Random(System.currentTimeMillis())

    @PostMapping("/process")
    fun processPayment(@RequestBody request: PaymentRequestDto): ResponseEntity<PaymentResponseDto> {

        log.info("Обработка оплат для заказа id: ${request.orderId}, сумма: ${request.amount}")



        if (failureMode.get()) {
            val randomRequest = random.nextInt(100)

            //Имитация ошибок
            if (randomRequest<60) {

//                log.error("Симуляция ошибки оплаты заказа id: ${request.orderId}")
//
//                return ResponseEntity
//                    .status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(PaymentResponseDto(false, "Сервис оплаты недоступен"))

                log.error("Симуляция замедления оплаты заказа id ${request.orderId}")

                Thread.sleep(5000)
            }
        }

        return ResponseEntity.ok(PaymentResponseDto(true, "Оплата проведена", false))

    }

    @GetMapping("admin/failure-mode")
    fun setFailureMode(@RequestParam enabled: Boolean) {

        failureMode.set(enabled)

        log.info("Failure mode в Payment Service, переключен на $enabled")
    }

}