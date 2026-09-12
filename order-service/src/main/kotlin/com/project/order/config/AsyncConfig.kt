package com.project.order.config

import lombok.RequiredArgsConstructor
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
@RequiredArgsConstructor
class AsyncConfig(
    private val tracingTaskDecorator: TracingTaskDecorator // Декоратор для проброса контекста трассировки в асинхронные задачи
): AsyncConfigurer {


    override fun getAsyncExecutor(): Executor {
        val executor: ThreadPoolTaskExecutor= ThreadPoolTaskExecutor()
        // Минимальное количество потоков, которые всегда держатся в пуле
        executor.corePoolSize=5
        // Максимальное количество потоков, которое может быть создано при пиковой нагрузке
        executor.maxPoolSize=10
        // Размер очереди задач, ожидающих выполнения, когда все потоки заняты
        executor.queueCapacity=100
        // Префикс имени потоков для удобной идентификации в логах и мониторинге
        executor.setThreadNamePrefix("async-")
        // При завершении приложения ждать завершения всех запущенных задач
        executor.setWaitForTasksToCompleteOnShutdown(true)
        // Максимальное время ожидания завершения задач при graceful shutdown (сек)
        executor.setAwaitTerminationSeconds(30)
        // Декоратор задач: позволяет пробросить MDC, traceId и spanId в новые потоки
        executor.setTaskDecorator(tracingTaskDecorator)
        executor.initialize()
        return executor
    }

}