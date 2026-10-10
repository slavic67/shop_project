package com.notification.config

import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.listener.ListenerExecutionFailedException
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConsumerRabbitConfig {

    private val log = LoggerFactory.getLogger(ConsumerRabbitConfig::class.java)

    companion object {
        const val QUEUE_NOTIFICATIONS: String = "notifications.queue"
        const val QUEUE_NOTIFICATIONS_DLQ: String = "notifications.queue.dlq"
        const val QUEUE_NOTIFICATIONS_RETRY: String = "notifications.retry"
        const val EXCHANGE_NOTIFICATIONS: String = "notifications.exchange"
        const val DLX_EXCHANGE: String = "notifications.dlx"
        const val ROUTING_KEY_NOTIFICATIONS: String = "notifications.queue"
        const val ROUTING_KEY_DLQ: String = "notifications.queue.dlq"
    }

    // ====== Обменники (для авономности) =====

    @Bean
    fun notificationsExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NOTIFICATIONS, true, false)
    }

    @Bean
    fun notificationsDlxExchange(): DirectExchange {
        return DirectExchange(DLX_EXCHANGE, true,false)
    }

    // ===== Очереди =====
    @Bean
    fun notificationsQueue(): Queue {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
            .withArgument("x-message-ttl", 3_600_000)      // 1 час
            .withArgument("x-max-length", 10_000)          // макс длина
            .withArgument("x-overflow", "reject-publish")  // при переполнении – отказ
            .build()
    }

    @Bean
    fun notificationsDlq(): Queue {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_DLQ).build()
    }

    @Bean
    fun notificationsRetryQueue(): Queue {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_RETRY)
            .withArgument("x-dead-letter-exchange", EXCHANGE_NOTIFICATIONS)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY_NOTIFICATIONS)
            .withArgument("x-message-ttl", 30_000) // 30 секунд задержки
            .build();
    }

    // ===== Привязки =====
    @Bean
    fun notificationsBinding(): Binding {
        return BindingBuilder.bind(notificationsQueue())
            .to(notificationsExchange())
            .with(ROUTING_KEY_NOTIFICATIONS);
    }

    @Bean
    fun dlqBinding(): Binding {
        return BindingBuilder.bind(notificationsDlq())
            .to(notificationsDlxExchange())
            .with(ROUTING_KEY_DLQ)
    }

    @Bean
    fun rabbitListenerErrorHandler(): RabbitListenerErrorHandler =
        { amqpMessage: Message,
          _,
          _,
          exception: ListenerExecutionFailedException ->

            // Причина ошибки — внутри ListenerExecutionFailedException
            val rootCause: Throwable = exception.cause ?: exception

            val props = amqpMessage.messageProperties
            log.error(
                "Ошибка обработки сообщения: messageId={}, routingKey={}, errorType={}, errorMessage={}",
                props.messageId,
                props.receivedRoutingKey,
                rootCause.javaClass.simpleName,
                rootCause.message
            )

            // ===== Гибкая логика обработки ошибок =====
            // when (rootCause) {
            //     is InvalidEventException ->
            //         throw AmqpRejectAndDontRequeueException("Invalid event data", rootCause)
            //     is MailServerUnavailableException ->
            //         throw rootCause // Spring Retry отработает, потом requeue/DLQ
            // }

            // По умолчанию: пробрасываем исключение → срабатывает retry → DLQ
            throw exception
        }



}