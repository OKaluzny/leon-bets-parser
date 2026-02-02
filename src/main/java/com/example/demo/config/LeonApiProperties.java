package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "leon")
public record LeonApiProperties(
        Api api,
        Parser parser
) {
    public record Api(
            String baseUrl,
            Duration timeout,
            Retry retry
    ) {
        public record Retry(
                int maxAttempts,
                Duration delay
        ) {}
    }

    public record Parser(
            int maxParallelRequests,
            int matchesPerLeague,
            List<String> targetSports
    ) {}
}
