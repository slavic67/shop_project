package com.project.order.config

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import jakarta.annotation.PostConstruct
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
@RequiredArgsConstructor
class Resilience4jConfig(

    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val bulkheadRegistry: BulkheadRegistry

    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun registerEventListeners() {

        circuitBreakerRegistry.circuitBreaker("paymentService")
            .eventPublisher
            .onSuccess { event ->
                log.info("CB SUCCESS: время выполнения ${event.elapsedDuration.toMillis()} ms")
            }
            .onError { event->
                log.error("CB ERROR: время выполнения ${event.elapsedDuration.toMillis()} ms, ошибка: ${event.throwable.message}")
            }
            .onStateTransition { event ->
                log.warn("CB STATE TRANSITION: ${event.stateTransition.fromState} ->  ${event.stateTransition.toState}")
            }
            .onCallNotPermitted { event ->
                log.warn("CB CALL NOT PERMITTED: вызов заблокировани (breaker открыт)")
            }
            .onIgnoredError { event ->
                log.debug("CB IGNORED ERROR: ошибка проигнорирована (не влияет на статистику): ${event.throwable.message}")
            }


        retryRegistry.retry("paymentService")
            .eventPublisher
            .onRetry { event ->
                log.debug("RETRY: попытка ${event.numberOfRetryAttempts} для ${event.name}")
            }
            .onSuccess { event ->
                log.info("SUCCESS: после ${event.numberOfRetryAttempts} попыток")
            }
            .onError { event->
                log.error("ERROR: попытка ${event.numberOfRetryAttempts}, причина: ${event.lastThrowable.message}")
            }


        bulkheadRegistry.bulkhead("paymentService")
            .eventPublisher
            .onCallPermitted { event ->
                log.debug("BULKHEAD: вызов разрешен: {}", event)
            }
            .onCallFinished { event ->
                log.debug("BULKHEAD: вызов ЗАКОНЧЕН: {}", event)
            }
            .onCallRejected { event ->
                log.warn("BULKHEAD: вызов ОТКЛОНЕН: {}", event)
            }
    }

}