package com.project.order.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

@Configuration
class HibernateConfig {

    @Bean
     fun jsonFormatMapperCustomizer( jsonMapper: JsonMapper): HibernatePropertiesCustomizer {
        return {props -> props[AvailableSettings.JSON_FORMAT_MAPPER] = Jackson3JsonFormatMapper(jsonMapper);
        }
    }
}