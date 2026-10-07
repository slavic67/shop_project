package com.project.order.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class WebClientConfig {
    @Value("\${notification-service.url}")
    private var notificationUrl: String?=null

    @Value("\${payment-service.url}")
    private var paymentUrl: String?=null

    @Bean
    fun notificationWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(notificationUrl!!).build()
    }

    @Bean
    fun paymentWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(paymentUrl!!).build()
    }

    ////    Нет метрик, нет интеграции с Retry/Circuit Breaker, статическая настройка.
    ////    вариант настройки timeout через клиента
    //    @Bean
    //    public WebClient paymentWebClient(WebClient.Builder builder) {
    //
    //        HttpClient httpClient = HttpClient.create()
    //                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)  // 1 сек на соединение
    //                .responseTimeout(Duration.ofSeconds(2));              // 2 сек на ожидание ответа
    //
    //        return builder
    //                .clientConnector(new ReactorClientHttpConnector(httpClient))
    //                .baseUrl(paymentUrl)
    //                .build();
    //    }

    //    настройка для RestTemplate

    //    @Bean
    //    public RestTemplate restTemplate() {
    //        RequestConfig config = RequestConfig.custom()
    //                .setConnectTimeout(1000)   // таймаут соединения (мс)
    //                .setConnectionRequestTimeout(1000)  // ожидание из пула
    //                .setSocketTimeout(2000)    // ожидание данных (мс)
    //                .build();
    //
    //        CloseableHttpClient client = HttpClientBuilder.create()
    //                .setDefaultRequestConfig(config)
    //                .build();
    //
    //        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
    //    }
}