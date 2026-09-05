package com.project.order.demo.repository

import com.project.order.Order
import com.project.order.OrderItem
import com.project.order.OrderStatus
import lombok.RequiredArgsConstructor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import kotlin.time.Instant

// Аннотация @Repository отмечает класс как компонент Spring для работы с данными
// "jdbcOrderRepository" - имя бина, по которому можно инжектить эту реализацию
@Repository("jdbcOrderRepository")
@RequiredArgsConstructor
class JdbcOrderRepository(
    // JdbcTemplate - основная абстракция Spring для работы с JDBC
    // Инкапсулирует работу с соединениями, обработку исключений, выполнение запросов
    private val jdbcTemplate: JdbcTemplate
) {

    companion object {

        // SQL запрос для вставки записи в таблицу orders
        // ? - плейсхолдеры для параметров, защита от SQL-инъекций
        private const val SET_ORDER: String = """
                INSERT INTO orders (status, create_at, order_number) 
                VALUES (?, ?, ?)
                """

        // SQL запрос для вставки записи в таблицу order_items
        private const val SET_ORDER_ITEMS: String = """
            INSERT INTO order_items 
            (order_id, product_id, product_name, quantity, price) 
            VALUES (?, ?, ?, ?, ?)
            """;
    }

    // RowMapper - интерфейс для преобразования строки ResultSet в Java-объект
    // Каждая строка результата запроса маппится в объект Order
    private val orderRowMapper: RowMapper<Order?> = RowMapper { rs: ResultSet?, rowNum: Int ->
        // Создаем новый пустой объект Order
        val order: Order = Order()


        // Устанавливаем ID из колонки "id" ResultSet
        order.id=rs!!.getLong("id")

        // Преобразуем строку статуса в enum OrderStatus
        order.status=OrderStatus.valueOf(rs.getString("status"))

        // Получаем номер заказа как строку
        order.orderNumber = rs.getString("order_number")

        // Получаем timestamp и конвертируем в Instant
        // PostgreSQL хранит create_at как TIMESTAMP WITH TIME ZONE
        order.createAt = rs.getTimestamp("create_at").toInstant()
        order
    }

    // RowMapper для преобразования строки ResultSet в OrderItem
    private val itemRowMapper: RowMapper<OrderItem> = RowMapper {rs: ResultSet?, rowNum: Int ->
        val item: OrderItem = OrderItem()
        item.id=rs!!.getLong("id")
        item.productId=rs.getLong("product_id")
        item.productName=rs.getString("product_name")
        item.quantity=rs.getInt("quantity")
        item.price=rs.getBigDecimal("price")
        item
    }

    // @Transactional гарантирует, что метод выполняется в транзакции
    // Если произойдет исключение - все изменения откатятся
    fun save(order: Order): Order{
        // 1. СОХРАНЕНИЕ ORDER

        // KeyHolder - механизм Spring для получения сгенерированных ключей
        // После INSERT запроса позволяет получить автоинкрементный ID
        val keyHodler: KeyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({
            // PreparedStatementCreator - лямбда для создания PreparedStatement
            connection ->
            // Создаем PreparedStatement с флагом RETURN_GENERATED_KEYS
            // Это указывает драйверу вернуть сгенерированные ключи
            val ps: PreparedStatement = connection.prepareStatement(
                SET_ORDER,
                Statement.RETURN_GENERATED_KEYS
            )

            // Устанавливаем параметры в плейсхолдеры (начинаются с 1)
            // 1-й параметр: статус заказа как строка (значение enum)
            ps.setString(1, order.status?.name)


            // 2-й параметр: дата создания как Timestamp
            // Instant конвертируется в Timestamp для JDBC
            ps.setTimestamp(2, Timestamp.from(order.createAt as java.time.Instant?))


            // 3-й параметр: номер заказа (UUID как строка)
            ps.setString(3, order.orderNumber)

            ps
        },
        // KeyHolder будет заполнен сгенерированными ключами
        keyHodler)


        // PostgreSQL возвращает всю строку, а не только ID
        // Получаем ID из возвращенной мапы
        val keys: MutableMap<String, Any>? = keyHodler.keys

        var orderId: Long? = null

        keys?.let { keys ->
            // PostgreSQL возвращает мапу с колонками
            // Пытаемся получить ID по имени колонки "id"
            if (keys.containsKey("id")) {
                // Приводим Object к Number, затем к Long
                orderId = (keys["id"] as Number).toLong()
            }

            // Альтернативный вариант: ищем числовой ID среди всех значений
            // Это fallback на случай, если драйвер вернет данные в другом формате
            for (value in keys.values) {
                if (value is Number) {
                    orderId = value.toLong()
                    break
                }
            }
        }

        // Проверка, что ID был успешно получен
        if (orderId == null) {
            throw RuntimeException("Failed to get generated order ID")
        }

        // Устанавливаем полученный ID в объект Order
        order.id = orderId

        // 2. СОХРАНЕНИЕ ORDERITEMS (BATCH INSERT)
        // Проверяем, что в заказе есть позиции
        if (!order.items.isEmpty()) {

            // Создаем список аргументов для batch-запроса
            // Каждый элемент массива соответствует одному плейсхолдеру в SQL
            val batchArgs: List<Array<Any?>> = order.items.stream().map { item ->
                arrayOf<Any?> (
                    orderId,
                    item.productId,
                    item.productName,
                    item.quantity,
                    item.price
                )
            }.toList()

            // batchUpdate выполняет один SQL для всех записей
            // Эффективнее, чем выполнять отдельные INSERT для каждой позиции
            jdbcTemplate.batchUpdate(SET_ORDER_ITEMS, batchArgs)

        }

        return order
    }

}