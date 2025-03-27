package com.k8s.agent.config

import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration


/**
 * Configuration for REST clients used in the application.
 * Configures timeout settings for RestTemplate instances.
 */
@Configuration
open class RestClientConfig {

    @Bean
    open fun restClientCustomizer(): RestClientCustomizer {
        return RestClientCustomizer { restClientBuilder: RestClient.Builder ->
            restClientBuilder
                .requestFactory(
                    ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                            .withConnectTimeout(Duration.ofSeconds(100))
                            .withReadTimeout(Duration.ofSeconds(600))
                    )
                )
        }
    }
}
