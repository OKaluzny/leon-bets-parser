package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.Event;
import com.example.demo.model.EventsResponse;
import com.example.demo.model.Sport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Collections;
import java.util.List;

@Service
public class LeonApiService {

    private static final Logger LOG = LoggerFactory.getLogger(LeonApiService.class);

    private static final String CTAG = "en-US";
    private static final String FLAGS = "reg,urlv2,mm2,rrc,nodup";
    private static final int SERVER_ERROR_THRESHOLD = 500;
    private static final int TOO_MANY_REQUESTS = 429;

    private final WebClient webClient;
    private final Retry retrySpec;
    private final CircuitBreaker circuitBreaker;
    private final boolean circuitBreakerEnabled;

    public LeonApiService(WebClient webClient, LeonApiProperties properties) {
        this.webClient = webClient;
        this.retrySpec = Retry.backoff(
                properties.api().retry().maxAttempts(),
                properties.api().retry().delay()
        ).filter(this::isRetryableException);

        LeonApiProperties.Api.CircuitBreaker cbConfig = properties.api().circuitBreaker();
        this.circuitBreakerEnabled = cbConfig.enabled();
        this.circuitBreaker = createCircuitBreaker(cbConfig);
    }

    private CircuitBreaker createCircuitBreaker(LeonApiProperties.Api.CircuitBreaker cbConfig) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.failureRateThreshold())
                .slidingWindowSize(cbConfig.slidingWindowSize())
                .waitDurationInOpenState(cbConfig.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cbConfig.permittedNumberOfCallsInHalfOpenState())
                .build();

        CircuitBreaker cb = CircuitBreaker.of("leonApi", config);

        cb.getEventPublisher()
                .onStateTransition(event ->
                        LOG.warn("Circuit breaker state changed: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()));

        return cb;
    }

    public Mono<List<Sport>> getSports() {
        String path = "/api-2/betline/sports";
        Mono<List<Sport>> request = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("ctag", CTAG)
                        .queryParam("flags", "urlv2")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch sports",
                                response.statusCode().value(), path)))
                .bodyToMono(new ParameterizedTypeReference<List<Sport>>() { })
                .retryWhen(retrySpec)
                .doOnError(e -> LOG.error("Error fetching sports", e))
                .onErrorReturn(Collections.emptyList());

        return applyCircuitBreaker(request);
    }

    public Mono<EventsResponse> getEventsByLeague(long leagueId) {
        String path = "/api-2/betline/events/all";
        Mono<EventsResponse> request = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("ctag", CTAG)
                        .queryParam("league_id", leagueId)
                        .queryParam("hideClosed", "true")
                        .queryParam("flags", FLAGS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch events for league " + leagueId,
                                response.statusCode().value(), path)))
                .bodyToMono(EventsResponse.class)
                .retryWhen(retrySpec)
                .doOnError(e -> LOG.warn("Error fetching events for league {}: {}", leagueId, e.getMessage()))
                .onErrorReturn(new EventsResponse());

        return applyCircuitBreaker(request);
    }

    public Mono<Event> getEventDetails(long eventId) {
        String path = "/api-2/betline/event/all";
        Mono<Event> request = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("ctag", CTAG)
                        .queryParam("eventId", eventId)
                        .queryParam("flags", FLAGS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch event " + eventId,
                                response.statusCode().value(), path)))
                .bodyToMono(Event.class)
                .retryWhen(retrySpec)
                .doOnError(e -> LOG.warn("Error fetching event {}: {}", eventId, e.getMessage()))
                .onErrorResume(e -> Mono.empty());

        return applyCircuitBreaker(request);
    }

    private <T> Mono<T> applyCircuitBreaker(Mono<T> mono) {
        if (circuitBreakerEnabled) {
            return mono.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }
        return mono;
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status >= SERVER_ERROR_THRESHOLD || status == TOO_MANY_REQUESTS;
        }
        return throwable instanceof ApiException apiEx && apiEx.isRetryable();
    }

    public static class ApiException extends RuntimeException {

        private final int statusCode;
        private final String url;

        public ApiException(String message, int statusCode, String url) {
            super(formatMessage(message, statusCode, url));
            this.statusCode = statusCode;
            this.url = url;
        }

        public ApiException(String message) {
            this(message, 0, null);
        }

        private static String formatMessage(String message, int statusCode, String url) {
            StringBuilder sb = new StringBuilder(message);
            if (statusCode > 0) {
                sb.append(" [status=").append(statusCode).append("]");
            }
            if (url != null && !url.isEmpty()) {
                sb.append(" [url=").append(url).append("]");
            }
            return sb.toString();
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getUrl() {
            return url;
        }

        public boolean isRetryable() {
            return statusCode >= SERVER_ERROR_THRESHOLD || statusCode == TOO_MANY_REQUESTS || statusCode == 0;
        }
    }
}
