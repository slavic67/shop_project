package com.project.order.config

import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import lombok.RequiredArgsConstructor
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component

@Component
@RequiredArgsConstructor
class TracingTaskDecorator(
    private val tracer: Tracer,
): TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        // Сохраняем текущий span
        val currentSpan: Span? = tracer.currentSpan()


        return Runnable {
            // Восстанавливаем span в новом потоке
            if (currentSpan != null) {
                // withSpan() возвращает Scope, который нужно закрыть
                tracer.withSpan(currentSpan).use { scope ->
                    // Внутри этого блока currentSpan считается активным
                    runnable.run()
                }  // здесь scope закрывается автоматически
            } else {
                runnable.run()
            }
        }
    }
}