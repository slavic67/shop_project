package com.project.order.metrics.aspect

import com.project.order.metrics.annotation.BusinessMetric
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import lombok.RequiredArgsConstructor
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit


@Aspect
@Component
@RequiredArgsConstructor
class BusinessMetricsAspect(
    private val meterRegistry: MeterRegistry
) {

    // Кэшируем Timer по имени + класс + статичные теги + статус
    // Если нужен p95 отдельно для success/error — кэшируем по статусу
    private val timerCache: ConcurrentMap<String, Timer> = ConcurrentHashMap()

    private val  log = LoggerFactory.getLogger(javaClass)


    // @Around: advice, который полностью оборачивает метод.
    // "@annotation(metric)": применяется к методам с аннотацией @TimedBusinessMetric.
    // ProceedingJoinPoint: доступ к вызываемому методу.
    // TimedBusinessMetric: сама аннотация с параметрами (value, tags).
    @Around("@annotation(metric)")
    fun measure(
        joinPoint: ProceedingJoinPoint,
        metric: BusinessMetric
    ): Any? {

        // System.nanoTime(): высокоточный таймер для измерения интервалов.
        // НЕ связан с системным временем, только относительные замеры.
        // Точность: наносекунды (1/1_000_000_000 секунды).
        val startNanos: Long = System.nanoTime()

        // Статус по умолчанию. Если метод упадёт — изменим на "error".
        var status: String="success"

        try {
            // joinPoint.proceed(): вызываем реальный метод (createOrder, getOrder и т.д.).
            // Возвращаем результат вызывающему коду.
            return joinPoint.proceed()
        } catch (e: Exception) {
            // Ловим любое исключение, помечаем статус ошибкой.
            // Перебрасываем исключение дальше — не проглатываем!
            status = "error";
            throw e;
        } finally {
            // finally: выполняется ВСЕГДА, даже при исключении.
            // Гарантируем запись метрики в любом случае.

            // Вычисляем прошедшее время: текущее минус стартовое.
            val durationNanos: Long = System.nanoTime() - startNanos

            // Вызываем запись метрик (Counter + Timer).
            recordMetrics(metric, joinPoint, durationNanos, status)
        }
    }


    // Приватный метод записи метрик. Выделен для читаемости.
    // metric: аннотация с параметрами.
    // joinPoint: доступ к контексту вызова (имя класса).
    // durationNanos: измеренное время.
    // status: "success" или "error".
    private fun recordMetrics(
        metric: BusinessMetric,
        joinPoint: ProceedingJoinPoint,
        durationNanos: Long,
        status: String
    ) {
        // try-catch: метрики НИКОГДА не должны ломать бизнес-логику.
        // Даже если запись метрики упадёт — метод уже отработал.
        try {

            // Получаем имя метрики из аннотации: "orders.created", "orders.retrieved".
            val metricName: String= metric.value;

            // Получаем имя класса через reflection: "OrderService".
            // joinPoint.getTarget(): проксированный бин (наш сервис).
            // getClass().getSimpleName(): "OrderService" (без package).
            val className: String = joinPoint.target.javaClass.simpleName

            // Tags.of(): создаём неизменяемый набор тегов.
            // "status": динамический тег (success/error).
            // "class": статичный тег (имя класса для фильтрации в Grafana).
            var allTags: Tags = Tags.of("status", status, "class", className)

            // Добавляем кастомные теги из аннотации: @TimedBusinessMetric(tags = {"type=write"}).
            allTags = addCustomTags(allTags, metric.tags)

            // Counter.builder(): создаём билдер счётчика.
            // metricName + ".total": суффикс по соглашению (Prometheus-style).
            // .tags(allTags): привязываем теги (статус, класс, кастомные).
            // .description(): человекочитаемое описание для документации.
            // .register(meterRegistry): регистрируем в реестре (или получаем существующий).
            // .increment(): увеличиваем счётчик на 1.
            Counter.builder("$metricName.total")
                .tags(allTags)
                .description("Total calls")
                .register(meterRegistry)
                .increment()

            // Строим ключ для кэша Timer'ов.
            // Ключ включает ВСЕ параметры, влияющие на уникальность метрики.
            // Почему включаем status: чтобы p95 считался отдельно для success и error.
            val timerKey: String = buildTimerKey(metricName, className, metric.tags, status)

            // computeIfAbsent(): атомарная операция "получить или создать".
            // Если ключ есть — возвращаем кэшированный Timer.
            // Если нет — создаём через лямбду, кладём в кэш, возвращаем.
            // Потокобезопасно: ConcurrentHashMap гарантирует единственное создание.
            val timer: Timer = timerCache.computeIfAbsent(timerKey, {key: String? ->

                // Создаём теги заново (нельзя reuse allTags — билдер копирует).
                var timerTags = Tags.of("class", className, "status", status)

                timerTags = addCustomTags(timerTags, metric.tags)


                // Timer.builder(): создаём билдер таймера.
                // metricName + ".duration": суффикс по соглашению.
                // .tags(timerTags): теги для фильтрации в Grafana.
                // .description(): описание для документации.
                // .publishPercentileHistogram(): включаем расчёт p50, p95, p99.
                //   Без этого только count, sum, max — нет перцентилей!
                // .sla(): закомментировано — пороги для бакетов (будущая фича).
                // .register(meterRegistry): регистрация в реестре.
                Timer.builder("$metricName.duration")
                    .tags(timerTags)
                    .description("Execution duration")
                    .publishPercentileHistogram()
                    // SLA как "фича на будущее":
                    // .sla(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(500))
                    .register(meterRegistry);
            })

            // Записываем измеренное время.
            // durationNanos: время в наносекундах.
            // TimeUnit.NANOSECONDS: указываем единицу измерения.
            // Timer конвертирует в базовую единицу (секунды для Prometheus).
            timer.record(durationNanos, TimeUnit.NANOSECONDS)



        } catch (e: Exception) {
            // Логируем ошибку метрик, но НЕ прерываем выполнение.
            // metric.value(): имя метрики для диагностики.
            log.warn("Failed to record metrics for {}", metric.value, e);
        }

    }

    /**
     * Безопасное добавление кастомных тегов (обрабатывает = в значении)
     */
    // Приватный метод парсинга кастомных тегов из аннотации.
    // base: базовые теги (status, class).
    // tagExpressions: массив строк из аннотации: ["type=write", "priority=high"].
    // Возвращает: новый Tags с добавленными тегами.
    private fun addCustomTags(
        base: Tags,
        tagExpressions: Array<String>
    ) : Tags {

        // Если кастомных тегов нет — возвращаем базовые.
        if (tagExpressions.isEmpty()) {
            return base
        }

        // Начинаем с базовых тегов.
        var result:Tags =base

        // Перебираем все выражения тегов.
        for (tagExpr in tagExpressions) {

            // Ищем первый '=' в строке.
            // Не используем split() — он ломается на "key=value=with=equals".
            val equalsIndex: Int = tagExpr.indexOf('=')

            // Проверяем, что '=' есть и не в начале (ключ не пустой).
            if (equalsIndex > 0) {

                // Подстрока до '=': ключ. trim() убирает пробелы.
                val key: String = tagExpr.substring(0, equalsIndex).trim();

                // Подстрока после '=': значение (может содержать '=').
                val value: String =  tagExpr.substring(equalsIndex + 1).trim();

                // Tags.and(): создаёт НОВЫЙ неизменяемый Tags с добавленным тегом.
                // Старый Tags не модифицируется (immutable pattern).
                result = result.and(key, value);

            }
        }

        // Возвращаем результат со всеми тегами.
        return result
    }


    /**
     * Построение ключа для кэша Timer'а
     */
    // Приватный метод построения ключа для кэша Timer'ов.
    // Ключ должен быть уникальным для каждой комбинации параметров.
    // Параметры: имя метрики, имя класса, кастомные теги, статус.
    private fun buildTimerKey(
        metricName: String,  // "orders.created"
        className: String,  // "OrderService"
        customTags: Array<String>,  // ["type=write", "priority=high"]
        status: String  // "success" или "error"
    ): String{

        // StringBuilder: эффективная конкатенация строк.
        val key: StringBuilder = StringBuilder()

        // Добавляем имя метрики и класс.
        key.append(metricName).append('.').append(className)

        // Если есть кастомные теги — добавляем в ключ.
        if (customTags.isNotEmpty()) {

            // Клонируем массив — не модифицируем оригинал.
            val sorted: Array<String> = customTags.clone()

            // Сортируем для консистентности ключа.
            // ["b=2", "a=1"] и ["a=1", "b=2"] дадут одинаковый ключ.
            sorted.sort()

            // Добавляем отсортированные теги в ключ.
            sorted.forEach {tag -> key.append('.').append(tag)}
        }

        // Добавляем статус в конец ключа.
        // Это позволяет иметь отдельные Timer для success и error.
        key.append('.').append(status);

        // Пример результата: "orders.created.OrderService.type=write.priority=high.success"
        return key.toString()
    }
}