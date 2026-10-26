package com.project.order.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig(

    @Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String,

    @Value("\${app.kafka.retry.max-attempts:3}")
    private val retryMaxAttempts: Int
) {

    /**
     * Базовая конфигурация для всех продюсеров.
     * Включает надёжные настройки по умолчанию.
     */
    private fun baseProducerConfigs(): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers;
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java;
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java;
        props[ProducerConfig.ACKS_CONFIG] = "all";
        props[ProducerConfig.RETRIES_CONFIG] = retryMaxAttempts;
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true;
        props[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = 1;
        props[ProducerConfig.BATCH_SIZE_CONFIG] = 16384;
        props[ProducerConfig.LINGER_MS_CONFIG] = 5;
        props[ProducerConfig.BUFFER_MEMORY_CONFIG] = 33554432;
        props[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip";
        props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 30000;
        props[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = 120000;
        return props;
    }

    // ===================== НАДЁЖНЫЙ ПРОДЮСЕР (по умолчанию) =====================
    @Bean
    @Primary
    fun reliableProducerFactory(): ProducerFactory<String, Any> {
        return DefaultKafkaProducerFactory(baseProducerConfigs())
    }

    @Bean
    @Primary
    fun reliableKafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(reliableProducerFactory())
    }

    @Bean
    fun retryKafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        val kafkaTemplate = KafkaTemplate(producerFactory)
        kafkaTemplate.setObservationEnabled(true)
        return kafkaTemplate
    }

    // ===================== ТРАНЗАКЦИОННЫЙ ПРОДЮСЕР (exactly-once) =====================
    @Bean
    fun transactionalProducerFactory(): ProducerFactory<String, Any> {
        val props: MutableMap<String, Any> = baseProducerConfigs()
        props[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "order-service-tx-producer"
        // Идемпотентность уже true, но подчеркнём
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        val factory = DefaultKafkaProducerFactory<String, Any>(props)
        factory.setTransactionIdPrefix("order-tx-")
        return factory
    }

    @Bean
    fun transactionalKafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(transactionalProducerFactory())
    }

    // ===================== ВЫСОКОПРОИЗВОДИТЕЛЬНЫЙ ПРОДЮСЕР (для сравнения) =====================
    @Bean
    fun highThroughputProducerFactory(): ProducerFactory<String, Any> {
        val props: MutableMap<String, Any> = baseProducerConfigs()
        props[ProducerConfig.ACKS_CONFIG] = "1"
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = false
        props[ProducerConfig.BATCH_SIZE_CONFIG] = 65536
        props[ProducerConfig.LINGER_MS_CONFIG] = 20
        props[ProducerConfig.BUFFER_MEMORY_CONFIG] = 67108864
        props[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun highThroughputKafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(highThroughputProducerFactory())
    }

}