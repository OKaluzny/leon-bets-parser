package com.example.demo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "leon")
@Validated
public record LeonApiProperties(
        @Valid @NotNull Api api,
        @Valid @NotNull Parser parser
) {
    public record Api(
            @NotBlank String baseUrl,
            @NotNull Duration timeout,
            @Valid @NotNull Retry retry,
            @Valid @NotNull Http http,
            @Valid @NotNull CircuitBreaker circuitBreaker
    ) {
        public record Retry(
                @Min(1) int maxAttempts,
                @NotNull Duration delay
        ) { }

        public record Http(
                @NotBlank String userAgent,
                @Min(1) int maxInMemorySizeMb
        ) { }

        public record CircuitBreaker(
                boolean enabled,
                @Min(1) int failureRateThreshold,
                @Min(1) int slidingWindowSize,
                @NotNull Duration waitDurationInOpenState,
                @Min(1) int permittedNumberOfCallsInHalfOpenState
        ) { }
    }

    public record Parser(
            @Min(1) int maxParallelRequests,
            @Min(1) int matchesPerLeague,
            @NotEmpty List<@NotBlank String> targetSports
    ) { }
}
