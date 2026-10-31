package com.notification.demo

import io.camunda.client.annotation.JobWorker
import io.camunda.client.api.response.ActivatedJob
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class DecisionMaker {


    @JobWorker
    fun decision(job : ActivatedJob): Map<String, Any> {
        return mapOf("decision" to Random.nextBoolean())
    }
}