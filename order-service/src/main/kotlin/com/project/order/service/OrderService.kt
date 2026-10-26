package com.project.order.service

import com.project.order.dictionary.OrderStatus
import com.project.order.dictionary.OutboxStatus
import com.project.order.domain.Order
import com.project.order.domain.OrderItem
import com.project.order.domain.event.OutboxEvent
import com.project.order.domain.dto.CreateOrderRequest
import com.project.order.domain.event.OrderCreatedEvent
import com.project.order.domain.dto.OrderResponse
import com.project.order.domain.dto.PaymentResponseDto
import com.project.order.exception.variants.NotFoundOrderException
import com.project.order.exception.variants.OrderCreationException
import com.project.order.mapper.OrderMapper
import com.project.order.metrics.annotation.BusinessMetric
import com.project.order.outbox.DebeziumMetrics
import com.project.order.repository.OrderRepository
import com.project.order.repository.OutboxEventRepository
import io.micrometer.observation.annotation.Observed
import io.opentelemetry.api.trace.Span
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean


@Slf4j
@Service
@RequiredArgsConstructor
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderMapper: OrderMapper,
    private val paymentService: PaymentService,
    private val outboxEventRepository: OutboxEventRepository,
    private val debeziumMetrics: DebeziumMetrics
) {
    private val random = Random()
    private val failureMode: AtomicBoolean = AtomicBoolean(false)

    private val log = LoggerFactory.getLogger(javaClass)

    //timeout для защиты от долгих SQL-запросов или залипаний на уровне БД, если метод выполняется дольше 5 секунд — транзакция откатится
    //не контролирует вызовы внешних сервисов, только бд
    @Transactional(timeout = 30)
    @BusinessMetric(
        value = "orders.created",
        tags = ["operation=create", "type=write"]
    )
    @Observed(name="order.creation", contextualName = "create-order")
    fun createOrder(request: CreateOrderRequest): OrderResponse? {

        log.debug("В метод createOrder получен запрос: {}", request)

        try {

            // Симуляция ошибок
            runFail()

            // Создаем сущность заказа
            val order = Order()
            val items :List<OrderItem> = request.items.stream()
                .map {
                    OrderItem(
                        it.productId,
                        it.productName,
                        it.quantity,
                        it.price,
                        order
                    )
                }
                .toList()
            order.addItems(items)

            //сначала всегда PENDING, потому что не знаем как закончится
            order.status = OrderStatus.AWAITING_PAYMENT

            // Сохраняем заказ
            val savedOrder = orderRepository.saveAndFlush(order)

            log.info("Заказ {} сохранен с статусом AWAITING_PAYMENT", savedOrder.id);

            // Сохраняем в outbox
            val traceId: String = Span.current().getSpanContext().getTraceId();
            val spanId: String = Span.current().getSpanContext().getSpanId();
            val traceparent: String = String.format("00-%s-%s-01", traceId, spanId); // формат W3C

            val orderCreatedEvent: OrderCreatedEvent = OrderCreatedEvent.of(
                savedOrder.id!!,
                MDC.getCopyOfContextMap()
            )

            val outboxEvent= OutboxEvent(
                aggregateType = "Order",
                aggregateId = savedOrder.id.toString(),
                eventType = "OrderCreatedEvent",
                payload = orderCreatedEvent,
                traceId = traceId,
                spanId = spanId,
                status = OutboxStatus.NEW,
                traceParent = traceparent,
            )

            outboxEventRepository.save(outboxEvent)
            debeziumMetrics.incrementOutBoxCreated()

            log.debug("Отправлено инфо о заказе, id: ${savedOrder.id}")

            MDC.put("order_id", savedOrder.id.toString());
            MDC.put("total_amount", savedOrder.calculateTotalAmount().toString());
            MDC.put("order_status", savedOrder.status.name);

            log.debug("Все успешно сохранено")

            // Теги добавятся в order.creation span
            Span.current().setAttribute("orderId", order.id!!)

            return orderMapper.from(order)
        } catch (e: Exception) {
            e.printStackTrace()
            val cause: Throwable? = e.cause
            log.error("Ошибка при оформлении заказа {}", cause?.message)
            throw OrderCreationException("Error: $cause?.message")
        } finally {
            // Очищаем только свои поля, trace_id остаётся чтобы не засорять память
            MDC.remove("order_id");
            MDC.remove("total_amount");
            MDC.remove("order_status");
        }
    }

    @Transactional(readOnly = true)
    @BusinessMetric(
        value = "orders.retrieved",
        tags = ["operation=get", "type=read"]
    )
    @Observed(name="order.get", contextualName = "get-order")
    fun getOrderWithItems(id: Long) : OrderResponse {


        log.debug("В метод getOrderWithItems получен запрос поиска order по id: {}", id)

        val order = orderRepository.findWithItemsById(id).orElseThrow(
            { NotFoundOrderException("Order not found for id $id") },
        )

        log.debug("Результат успешно найден")
        return orderMapper.from(order)

    }

    fun setFailureMode(enabled: Boolean) {
            failureMode.set(enabled)
            log.info("Failure mode в OrderService, переключен на: $enabled")
    }

    private fun runFail() {
        if (failureMode.get()) {

            val randomInt: Int = random.nextInt(100)

            log.debug("Выпало число: $randomInt")

            if (randomInt <30) {
                log.error("Возникли проблемы с обработкой сохранения заказа")
                throw RuntimeException("Типа проблемы с обработкой заказа")
            }

            if (randomInt > 70) {
                log.warn("OrderService замедлился")
                Thread.sleep(200)
            }
        }
    }

    @Transactional
    fun completeOrder(orderId: Long) {
        val order: Order = orderRepository.findById(orderId).orElseThrow(
            { NotFoundOrderException("Order not found for id $orderId") },
        )

        // Проверяем, что заказ в ожидании оплаты
        if (order.status!= OrderStatus.AWAITING_PAYMENT) {
            log.warn("Order {} is not in AWAITING_PAYMENT state (current: {}), skipping", orderId, order.status);
            return;
        }

        order.status = OrderStatus.PAID

        orderRepository.save(order)

        log.info("Order {} completed (PAID)", orderId);
    }


    @Transactional
    fun cancelOrder(orderId: Long, reason: String) {
        val order: Order = orderRepository.findById(orderId).orElseThrow(
            { OrderCreationException("Order not found for id $orderId") },
        )

        if (order.status != OrderStatus.AWAITING_PAYMENT) {
            log.warn("Order {} is not in AWAITING_PAYMENT state (current: {}), skipping", orderId, order.status);
            return;
        }

        order.status = OrderStatus.CANCELLED

        orderRepository.save(order)

        log.info("Order {} cancelled due to: {}", orderId, reason);
    }
}