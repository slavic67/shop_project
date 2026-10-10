package com.notification.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.notification.config.ConsumerRabbitConfig
import com.notification.event.OrderCreatedEvent
import com.rabbitmq.client.Channel
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class NotificationConsumer(
    private val objectMapper : ObjectMapper
) {

    private val log= LoggerFactory.getLogger(NotificationConsumer::class.java)


    @RabbitListener(
        // ===== ОСНОВНЫЕ: КАКИЕ ОЧЕРЕДИ СЛУШАТЬ =====
        id = "notificationConsumer",                              // Уникальный ID контейнера (для управления)
        containerFactory = "rabbitListenerContainerFactory",      // Фабрика контейнеров (по умолчанию)
        queues = [ConsumerRabbitConfig.QUEUE_NOTIFICATIONS],        // Имена очередей (можно массив)
        // queuesToDeclare = {},                                  // Очереди, которые нужно объявить прямо здесь
        bindings = [],                                            // Привязки, если объявляем очереди в аннотации
        // group = "notifications-group",                         // Группа потребителей (для конкурирующих consumer'ов)

        // ===== АДМИНИСТРАТИВНЫЕ =====
        autoStartup = "true",                                     // Автозапуск контейнера (по умолчанию true)
        // admin = "rabbitAdmin",                                 // Бин RabbitAdmin (если несколько)

        // ===== ПОДТВЕРЖДЕНИЕ (ACK) =====
        ackMode = "MANUAL",                                         // Режим подтверждения: AUTO / MANUAL / NONE

        // ===== КОНКУРЕНТНОСТЬ =====
        concurrency = "3",                                        // Мин. число потребителей (потоков)
        // maxConcurrency = "10",                                 // Макс. число потоков (можно в YAML)

        // ===== ПРЕДВЫБОРКА =====
        // prefetch = "10",                                       // Prefetch count (можно в YAML)

        // ===== ПАКЕТНАЯ ОБРАБОТКА =====
        // batchSize = "50",                                      // Размер пакета для batch-обработки

        // ===== ОБРАБОТКА ОШИБОК =====
        errorHandler = "rabbitListenerErrorHandler",              // Бин обработчика ошибок
        // returnExceptions = "false",                            // Пробрасывать исключения в вызывающий код

        // ===== УПРАВЛЕНИЕ СОЕДИНЕНИЕМ =====
        exclusive = false                                         // Эксклюзивный доступ к очереди
        // priority = "0",                                        // Приоритет consumer'а

        // ===== ОТВЕТНЫЕ СООБЩЕНИЯ (RPC — НЕ ИСПОЛЬЗУЕМ) =====
        // replyContentType = "application/json",                 // Content-Type ответа
        // replyPostProcessor = "replyPostProcessor",             // Постобработчик ответа

        // ===== ДОПОЛНИТЕЛЬНЫЕ =====
        // messageConverter = "messageConverter",                 // Конвертер сообщений (по умолчанию Jackson2Json)
        // executor = "taskExecutor",                             // Исполнитель потоков
        // contentType = "application/json"                       // Ожидаемый Content-Type
    )
    fun handle(
        body: ByteArray,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        log.info("Приняты сырые данные")

        val event: OrderCreatedEvent = objectMapper.readValue(body, OrderCreatedEvent::class.java)

        try {
            log.info("Принято событие из RabbitMQ: orderId = ${event.orderId}")

            //            для демонстрации наращивания очереди
//            Thread.sleep(2000)

            // для демонстрации отправки сообщений в DLQ
//            throw RuntimeException("GO EXCEPTION");

            channel.basicAck(deliveryTag, false)
            log.info("Acknowledged: orderId = ${event.orderId}")

            log.info("Email пл заказу: orderId = ${event.orderId}, успешно отправлен")

        } catch (e: Exception) {
            log.error("Processing failed: orderId=${event.orderId}, error=${e.message}")

            //Отклонен без возврата в очередь: poison -> DLQ
            // Параметры: deliveryTag, multiple, requeue
            channel.basicNack(deliveryTag, false, false);
            // requeue=false → сообщение уйдёт в DLX/DLQ
        }

    }

    //пример с ненастроенной сериализацией/ошибка
    fun handle( event: OrderCreatedEvent) {
        log.info("Принято событие из RabbitMQ: orderId={}", event.orderId);

        log.info("Email по заказу: orderId={}, успешно отправлен", event.orderId);
    }


}