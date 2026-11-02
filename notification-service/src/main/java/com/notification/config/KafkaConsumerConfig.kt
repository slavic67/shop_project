package com.notification.config


import com.fasterxml.jackson.databind.ObjectMapper
import com.notification.event.NotificationEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer


@Configuration
@EnableKafka
class KafkaConsumerConfig(
    @Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String,

    @Value("\${spring.kafka.consumer.group-id}")
    private val groupId: String,
) {


    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {

        val props: MutableMap<String, Any> = HashMap()

        // Адреса брокеров Kafka (из YAML)
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers;

        // Идентификатор consumer group (обязателен для группового управления offset)
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId;

        // С какой позиции начинать чтение, если offset не задан: earliest — с самого начала
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest";

        // Отключаем авто-коммит offset — будем подтверждать вручную после успешной обработки
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false;

        // Десериализатор для ключа (обычно String, например, orderId)
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java;
        props[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = ErrorHandlingDeserializer::class.java;

        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java;
        props[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JacksonJsonDeserializer::class.java;

        props[JacksonJsonDeserializer.TYPE_MAPPINGS] = """
            notificationEvent:${NotificationEvent::class.java.name}
            """;
        props[JacksonJsonDeserializer.TRUSTED_PACKAGES] = "com.notification.";

//        // Десериализатор для значения — принимаем сырой массив байт
//        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java;

        // При необходимости здесь можно добавить дополнительные свойства:
        // - MAX_POLL_RECORDS_CONFIG (количество записей за один вызов poll)
        // - MAX_POLL_INTERVAL_MS_CONFIG (максимальный интервал между poll)
        // - SESSION_TIMEOUT_MS_CONFIG и другие

        return DefaultKafkaConsumerFactory(props);
    }

    /**
     * Фабрика контейнеров слушателей (ConcurrentKafkaListenerContainerFactory).
     * Настраивает ручной коммит (MANUAL_IMMEDIATE), чтобы подтверждать offset
     * только после того, как бизнес-логика выполнена успешно.
     *
     * @return фабрика, которую будет использовать @KafkaListener
     */
    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, Any>):
            ConcurrentKafkaListenerContainerFactory<String, Any> {

        val factory: ConcurrentKafkaListenerContainerFactory<String, Any> =
            ConcurrentKafkaListenerContainerFactory()

        factory.setConsumerFactory(consumerFactory)

        // Ручное подтверждение: сообщение должно быть подтверждено через Acknowledgment.acknowledge()
        factory.containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Количество потребительских потоков (concurrency). Если топик имеет 3 партиции,
        // можно установить concurrency = 3 для параллельной обработки.
        factory.setConcurrency(1); // для демо достаточно одного потока

        // Здесь также можно задать обработчик ошибок (DefaultErrorHandler),
        // фильтры и другие параметры.

        return factory;

    }
}