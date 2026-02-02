package com.example.demo.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(LeonApiProperties.class)
public class WebClientConfig {

    private static final int BYTES_PER_MB = 1024 * 1024;

    @Bean
    public WebClient webClient(LeonApiProperties properties) {
        LeonApiProperties.Api apiConfig = properties.api();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(apiConfig.timeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) apiConfig.timeout().toMillis());

        int maxInMemorySize = apiConfig.http().maxInMemorySizeMb() * BYTES_PER_MB;

        return WebClient.builder()
                .baseUrl(apiConfig.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", apiConfig.http().userAgent())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();
    }
}
