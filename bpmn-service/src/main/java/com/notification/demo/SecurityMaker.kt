package com.notification.demo

import io.camunda.client.annotation.JobWorker
import io.camunda.client.api.response.ActivatedJob
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class SecurityMaker {

    @JobWorker
    fun info(job : ActivatedJob): Map<String, Any> {
        return mapOf("passNumber" to Random.nextInt(100_00, 1_000_000))
    }

    @JobWorker
    fun inside(job : ActivatedJob): Map<String, Any> {
        return mapOf("insideRisk" to Random.nextBoolean())
    }

    @JobWorker
    fun outside(job : ActivatedJob): Map<String, Any> {
        return mapOf("outsideRisk" to Random.nextBoolean())
    }

}