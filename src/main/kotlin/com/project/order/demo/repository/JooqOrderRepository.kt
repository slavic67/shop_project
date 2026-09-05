package com.project.order.demo.repository

import com.core.jooq.tables.OrderItems.ORDER_ITEMS
import com.core.jooq.tables.Orders.ORDERS
import com.core.jooq.tables.records.OrderItemsRecord
import com.core.jooq.tables.records.OrdersRecord
import com.project.order.Order
import lombok.RequiredArgsConstructor
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Repository
@RequiredArgsConstructor
class JooqOrderRepository(
    // DSLContext - основной интерфейс jOOQ для построения запросов
    // Это типобезопасный DSL (Domain Specific Language) для SQL
    private val dsl: DSLContext
) {


    fun save(order: Order): Order {

        // СОХРАНЕНИЕ ORDER С ИСПОЛЬЗОВАНИЕМ jOOQ RECORD

        // Создаем новый Record для таблицы orders
        // Record - это типобезопасное представление строки таблицы
        val orderRecord: OrdersRecord = dsl.newRecord(ORDERS)

        // Устанавливаем значение статуса
        // ORDERS.STATUS - это сгенерированное константное поле
        // Преобразуем enum OrderStatus в String
        orderRecord.status=order.status?.name


        val offsetDateTime: OffsetDateTime? = order.createAt?.atOffset(ZoneOffset.UTC)

        // Устанавливаем дату создания и номер заказа
        // Все setter'ы типобезопасны
        orderRecord.createdAt = offsetDateTime
        orderRecord.orderNumber= order.orderNumber


        // Сохраняем запись в базу данных
        // Метод store() выполняет INSERT или UPDATE в зависимости от состояния Record
        // После сохранения Record автоматически заполняется сгенерированным ID
        orderRecord.store()  // Сохраняем и получаем сгенерированный ID

        // Получаем сгенерированный ID из сохраненной записи
        val orderId: Long = orderRecord.id

        // Устанавливаем ID в исходный объект Order
        order.id = orderId

        // СОХРАНЕНИЕ ORDERITEMS (BATCH INSERT)
        // Проверяем, что в заказе есть позиции
        if (!order.items.isEmpty()) {

            // Создаем список Records для позиций заказа
            val itemRecords: List<OrderItemsRecord> = order.items.stream()
                .map { item ->

                    val record: OrderItemsRecord = dsl.newRecord(ORDER_ITEMS)

                    // Устанавливаем значения полей
                    // Все поля типобезопасны - компилятор проверит правильность
                    record.orderId = orderId  // Связь с родительским заказом
                    record.productId = item.productId  // ID продукта
                    record.productName = item.productName  // Название продукта
                    record.quantity = item.quantity // Количество
                    record.price = item.price  // Цена


                    record
                }.toList()  // Собираем в неизменяемый список

            dsl.batchInsert(itemRecords).execute()
        }

        // Возвращаем обновленный объект Order с установленным ID
        return order

    }
}