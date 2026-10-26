package com.project.order.outbox

import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForEntity


@Service
class KafkaConnectService(
    @Value("\${debezium.connect.url}")  val connectUrl: String,
    @Value("\${debezium.connector.name}")  val connectorName: String,
    @Value("\${debezium.connector.topics-prefix}") val topicsPrefix: String,
    @Value("\${debezium.connector.table-include-list}")  val tableIncludeList: String,
    @Value("\${debezium.connector.outbox-route-topic}")  val outboxRouteTopic: String,
    @Value("\${debezium.connector.plugin-name}")  val pluginName: String,
    @Value("\${debezium.database.host}")  val dbHost: String,
    @Value("\${debezium.database.port}")  val dbPort: Int,
    @Value("\${debezium.database.name}") val dbName: String,
    @Value("\${debezium.database.user}") val dbUser: String,
    @Value("\${debezium.database.password}")  val dbPassword: String,
    @Value("\${debezium.config.recreate}")  val configRecreate: Boolean,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val restTemplate = RestTemplate()
    private val restClient = RestClient.create(connectUrl)

    fun createOutboxConnectorIfNotExists() {
        val url= "$connectUrl/connectors"

        val config: Map<String, String> = buildDebeziumConfig()
        val request: HttpEntity<Map<String, Any>> = buildRequest(connectorName, config)

        if (connectorExists(connectorName)) {
            log.info("Connector $connectorName already exists")
            if (configRecreate) {
                restClient.delete()
                    .uri("/connectors/{name}", connectorName)
                    .retrieve()
                    .toBodilessEntity();
            } else {
                return
            }
        }


        try {
            val response: ResponseEntity<String> = restTemplate.postForEntity<String>(url, request);
            log.info("Connector created successfully: ");

        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                log.info("Connector already exists (409 Conflict) – idempotency check passed");
            } else {
                log.error("Failed to create connector: status={}, body={}", e.statusCode, e.responseBodyAsString, e);
                throw RuntimeException("Failed to create Debezium connector", e);
            }
        }

    }

    fun connectorExists(name: String): Boolean {
        val url = "$connectUrl/connectors/$name"
        try {
            val response: ResponseEntity<String> = restTemplate.getForEntity<String>(url)
            return response.statusCode == HttpStatus.OK
        } catch (e: HttpClientErrorException.NotFound) {
            return false
        } catch (e: Exception) {
            log.warn("Failed to check connector existence", e);
            return false
        }
    }

    fun connectorStatus(): String? =
        restTemplate.getForObject<String>("$connectUrl/connectors/$connectorName/status")

    fun buildRequest(connectorName: String, config: Map<String, String>): HttpEntity<Map<String, Any>> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val body: MutableMap<String, Any> = LinkedHashMap()
        body["name"] = connectorName
        body["config"]=config

        return HttpEntity(body, headers)
    }

    fun buildDebeziumConfig(): Map<String, String> {
        val config: MutableMap<String, String> = LinkedHashMap();
        config["connector.class"] = "io.debezium.connector.postgresql.PostgresConnector";
        config["database.hostname"] = dbHost;
        config["database.port"] = dbPort.toString();
        config["database.user"] = dbUser;
        config["database.password"] = dbPassword;
        config["database.dbname"] = dbName;
        // --- Настройки топиков Kafka ---
        // Префикс для всех топиков, которые создаёт коннектор (обычно имя сервера / приложения)
        config["topic.prefix"] = topicsPrefix;
        // Какие таблицы отслеживать (в формате "schema.table", можно несколько через запятую)
        config["table.include.list"] = tableIncludeList;
        // --- Механизм репликации PostgreSQL ---
        // Используем встроенный плагин pgoutput (доступен в PostgreSQL 10+, не требует установки расширений)
        config["plugin.name"] = pluginName;

        // --- Трансформации (SMT)
        // Список трансформаций (может быть несколько через запятую)
        config["transforms"] = "outbox";
        // Тип трансформации: встроенный обработчик для паттерна Outbox
        config["transforms.outbox.type"] = "io.debezium.transforms.outbox.EventRouter";
        // Структура outbox таблицы
        config["transforms.outbox.table.field.event.type"] = "event_type"
        config["transforms.outbox.table.field.event.key"] = "aggregate_id"
        config["transforms.outbox.route.by.field"] = "aggregate_type"

        // Имя целевого топика, в который будет отправлено сообщение (статический топик)
        config["transforms.outbox.route.topic.replacement"] = outboxRouteTopic;
        // Дополнительные поля из таблицы outbox, которые нужно положить в заголовки Kafka-сообщения.
        config["transforms.outbox.table.fields.additional.placement"] = "event_type:header,trace_parent:header";
        // Раскрывать JSON-поле payload как тело сообщения, а не как вложенную строку.
        // Если true, сообщение в Kafka будет чистым JSON из payload, без лишней обёртки.
        config["transforms.outbox.table.expand.json.payload"] = "true";
        return config;
    }

}