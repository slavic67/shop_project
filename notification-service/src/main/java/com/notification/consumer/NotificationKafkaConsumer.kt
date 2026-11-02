package com.notification.consumer

import com.notification.event.NotificationEvent
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@Component
@RequiredArgsConstructor
@RetryableTopic(
    attempts = "3",                                // Общее количество попыток (1 первая + 2 ретрая)
    backOff = BackOff(
        // Настройка задержек между попытками
        delay = 1000,                          // начальная задержка (мс)
        maxDelay = 10000,                      // максимальная задержка (если multiplier>1)
        multiplier = 2.0,                      // множитель для экспоненциального роста
    ),
    timeout = "60000",                          // Максимальное время всех попыток (после него -> DLT)
    retryTopicSuffix = "-retry",                   // Суффикс для ретрай-топиков (по умолчанию "-retry")
    dltTopicSuffix = ".DLT",                       // Суффикс для Dead Letter Topic (по умолчанию ".dlt")
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE, // Как именовать ретрай-топики:
    sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC, // Переиспользовать ли один топик для одинаковых задержек
    exclude = [IllegalArgumentException::class, NullPointerException::class], // Эти исключения -> сразу в DLT
    traversingCauses = "true",                     // Анализировать ли причину (cause) исключения при проверке include/exclude
    dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR, // Всегда ретраить до исчерпания attempts, затем DLT
    autoCreateTopics = "true",                     // Автоматически создавать ретрай-топики и DLT
    numPartitions = "1",                           // Количество партиций для создаваемых топиков, делаем 1 потому что топик 1
    replicationFactor = "1",                       // Фактор репликации (по умолчанию -1 = использовать настройки брокера), делаем 1 потому что топик 1
    listenerContainerFactory = "kafkaListenerContainerFactory",
    concurrency = "3"
)
@KafkaListener(
    topics = ["\${app.topic.notification-events}"],
    groupId = "notification-service-group",
    containerFactory = "kafkaListenerContainerFactory"
)
class NotificationKafkaConsumer {
    private val log = LoggerFactory.getLogger(NotificationKafkaConsumer::class.java)

    @KafkaHandler
    fun consumeResultProcessOrder(
        event: NotificationEvent,
        acknowledgment: Acknowledgment
    ) {
        try {
            // Бизнес-валидация
            requireNotNull(event.orderId) { "orderId must not be null" }


            if (event.transactionId != null) {
                log.info(
                    """
                    Отправлено уведомление:
                    Заказ: {}, успешно обработан.
                    Статус заказа: {}
                    Id транзакции: {}
                    """, event.orderId,
                    event.status,
                    event.transactionId
                )
            } else {
                log.info(
                    """
                    Отправлено уведомление:
                    Заказ: {}, не обработан.
                    Статус заказа: {}
                    Причина: {}
                    """, event.orderId,
                    event.status,
                    event.reason
                );
            }


            //Ручной коммит offset
            acknowledgment.acknowledge();
            log.info("Событие для заказа: {} обработано и оффсет для него сдвинут(acknowledged) ", event.orderId);
        } catch (e: Exception) {
            log.error("Error processing order", e)
            // Не вызываем acknowledgment - сообщение попадетв DLT после всех retry
            throw RuntimeException("Processing failed", e)
        }
    }
}