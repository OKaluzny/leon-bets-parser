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

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    @Bean
    public WebClient webClient(LeonApiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.api().timeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.api().timeout().toMillis());

        return WebClient.builder()
                .baseUrl(properties.api().baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", USER_AGENT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
