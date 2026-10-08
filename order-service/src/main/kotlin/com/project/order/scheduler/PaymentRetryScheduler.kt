package com.project.order.scheduler

import com.project.order.dictionary.OrderStatus
import com.project.order.domain.Order
import com.project.order.repository.OrderRepository
import com.project.order.service.PaymentService
import io.micrometer.core.instrument.MeterRegistry
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap


@Component
@RequiredArgsConstructor
class PaymentRetryScheduler(

    @Value($$"${payment.retry.batch-size:500}")
    private var batchSize: Int,

    @Value("\${payment.scanner.fixed-delay:30000}")
    private val fixedDelay: Long,

    @Value( $$"${payment.scanner.initial-delay:30000}")
    private val initDelay: Long,

    private val paymentRetryProcessor: PaymentRetryProcessor,

    private val orderRepository: OrderRepository,

) {

    private val retryCounters: Map<Long, Int> = ConcurrentHashMap()
        private val log = LoggerFactory.getLogger(PaymentRetryScheduler::class.java)

    /**
     * Периодическое сканирование и обработка pending заказов.
     *
     * <p><b>Настройки расписания:</b>
     * <ul>
     *   <li><b>fixedDelayString</b> - задержка МЕЖДУ ОКОНЧАНИЕМ текущего и НАЧАЛОМ следующего выполнения.
     *       Используется fixedDelay (а не fixedRate) чтобы избежать наложения задач друг на друга,
     *       что критично при работе с БД и внешними сервисами.</li>
     *   <li><b>initialDelayString</b> - задержка перед ПЕРВЫМ запуском после старта приложения.
     *       Нужна чтобы дать приложению полностью инициализироваться (поднять контекст,
     *       установить соединения с БД, прогреть кэши) до начала обработки.</li>
     * </ul>
     *
     * <p><b>Значения берутся из конфигурации с возможностью переопределения:</b>
     * <pre>
     * payment.scanner.fixed-delay - интервал между запусками в миллисекундах (по умолчанию 30000 = 30 сек)
     * payment.scanner.initial-delay - начальная задержка в миллисекундах (по умолчанию 30000 = 30 сек)
     * </pre>
     *
     * <p><b>Альтернативные варианты настройки:</b>
     * <pre>
     * // Фиксированная задержка без возможности переопределения
     * &#64;Scheduled(fixedDelay = 30000, initialDelay = 30000)
     *
     * // CRON выражение для гибкой настройки (например разное расписание днём и ночью)
     * &#64;Scheduled(cron = "${payment.scanner.cron:0/30 * * * * ?}")
     * </pre>
     *
     * <p><b>Гарантии выполнения:</b>
     * <ul>
     *   <li>Если задача выполняется дольше интервала - следующий запуск ждёт завершения</li>
     *   <li>При возникновении исключения - задача логирует ошибку и продолжает работу по расписанию</li>
     *   <li>Для параллельного выполнения нужен отдельный пул потоков (см. SchedulingConfig)</li>
     * </ul>
     *
     * @see #processOrder(Order) - обработка одного заказа
     * @see #isOrderExpired(Order) - проверка устаревания заказа
     */

    @Scheduled(
        fixedDelay = 30000,
        // задержка МЕЖДУ КОНЦОМ предыдущего и НАЧАЛОМ следующего выполнения
        // выбрана вместо fixedRate чтобы гарантировать, что задачи не накладываются
        initialDelay = 30000
        // задержка перед ПЕРВЫМ запуском после старта приложения
        // даёт время на полную инициализацию контекста Spring
    )
    @Transactional
    fun scanProcessPendingOrders() {
            log.debug("Старт поиска pending заказов")

        val limit: Pageable = PageRequest.of(0, batchSize)
        // Находим все заказы в статусе PENDING
        val pendingOrders: List<Order> = orderRepository.findByStatus(OrderStatus.PENDING, limit)

        if (pendingOrders.isEmpty()) {
            log.debug("Нет pending заказов для обработки")
            return
        }

        log.info("Найдено {} pending заказов", pendingOrders.size)

        //Обрабатываем каждый заказ
        for (order in pendingOrders) {
            try {
                paymentRetryProcessor.processOrder(order)
            } catch (e: Exception) {
                log.error("Ошибка при обработке заказа {}", order.id, e)
            }
        }

    }
}