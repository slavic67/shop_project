package com.notification.demo

import io.camunda.client.annotation.JobWorker
import io.camunda.client.api.response.ActivatedJob
import org.springframework.stereotype.Component
import kotlin.random.Random
import kotlin.random.nextInt


@Component
class ServiceMaker {

    @JobWorker
    fun partnerSystem(job : ActivatedJob): Map<String, Any> {
        return mapOf("reliable" to Random.nextBoolean())
    }

    @JobWorker
    fun publicSystem(job : ActivatedJob): Map<String, Any> {
        return mapOf("hasCredit" to Random.nextBoolean())
    }

    @JobWorker
    fun coreBanking(job : ActivatedJob): Map<String, Any> {
        return mapOf("countCredit" to Random.nextInt(10))
    }

    @JobWorker
    fun aml(job : ActivatedJob): Map<String, Any> {
        return mapOf("blackList" to Random.nextBoolean())
    }
}