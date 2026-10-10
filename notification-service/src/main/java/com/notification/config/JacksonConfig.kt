package com.notification.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfig {

//    @Bean
//    fun objectMapper() : ObjectMapper {
//        return ObjectMapper().registerKotlinModule()
//    }
//
//    @Bean
//    @Primary
//    fun jsonMapper(): JsonMapper {
//        return JsonMapper.builder()
//            .findAndAddModules()
//
//            .serializationInclusion(JsonInclude.Include.NON_NULL)
//            .build()
//
//    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(JavaTimeModule())
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

}