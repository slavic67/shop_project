package com.project.order.config

import feign.Feign
import feign.Request
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableFeignClients(basePackages = ["com.project.order.feignClient"])
class FeignConfig {

    @Bean
    fun feignBuilder(): Feign.Builder {
        return Feign.builder()
            .options(
                Request.Options(1, TimeUnit.SECONDS,
                    2L, TimeUnit.SECONDS, true)
            )
    }

}