package com

import io.camunda.client.annotation.Deployment
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@Deployment(resources = ["classpath*:/processes/**/*.bpmn"])
class BpmServiceApplication

fun main(args: Array<String>) {
    runApplication<BpmServiceApplication>(*args)
}
