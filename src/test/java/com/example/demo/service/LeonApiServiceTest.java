package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeonApiServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private LeonApiService apiService;

    @BeforeEach
    void setUp() {
        LeonApiProperties properties = new LeonApiProperties(
                new LeonApiProperties.Api(
                        "https://leon.bet",
                        Duration.ofSeconds(30),
                        new LeonApiProperties.Api.Retry(1, Duration.ofMillis(100)),
                        new LeonApiProperties.Api.Http("Mozilla/5.0 Test", 16),
                        new LeonApiProperties.Api.CircuitBreaker(false, 50, 10, Duration.ofSeconds(30), 3)
                ),
                new LeonApiProperties.Parser(3, 2, List.of("Soccer"))
        );
        apiService = new LeonApiService(webClient, properties);
    }

    @Test
    void getSports_returnsListOfSports() {
        // Given
        List<Sport> expectedSports = List.of(
                new Sport(1L, "Football", "Soccer", List.of()),
                new Sport(2L, "Tennis", "Tennis", List.of())
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(expectedSports));

        // When & Then
        StepVerifier.create(apiService.getSports())
                .assertNext(sports -> {
                    assertThat(sports).hasSize(2);
                    assertThat(sports.get(0).name()).isEqualTo("Football");
                    assertThat(sports.get(1).name()).isEqualTo("Tennis");
                })
                .verifyComplete();
    }

    @Test
    void getSports_onError_returnsEmptyList() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        // When & Then
        StepVerifier.create(apiService.getSports())
                .assertNext(sports -> assertThat(sports).isEmpty())
                .verifyComplete();
    }

    @Test
    void getEventsByLeague_returnsEventsResponse() {
        // Given
        Event event = new Event(1L, "Match A vs B", System.currentTimeMillis(), List.of());
        EventsResponse expectedResponse = new EventsResponse(List.of(event));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EventsResponse.class))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        StepVerifier.create(apiService.getEventsByLeague(123L))
                .assertNext(response -> {
                    assertThat(response.events()).hasSize(1);
                    assertThat(response.events().get(0).name()).isEqualTo("Match A vs B");
                })
                .verifyComplete();
    }

    @Test
    void getEventsByLeague_onError_returnsEmptyResponse() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EventsResponse.class))
                .thenReturn(Mono.error(new RuntimeException("API error")));

        // When & Then
        StepVerifier.create(apiService.getEventsByLeague(123L))
                .assertNext(response -> assertThat(response.events()).isEmpty())
                .verifyComplete();
    }

    @Test
    void getEventDetails_returnsEvent() {
        // Given
        Runner runner = new Runner(1L, "Home", 1.5, true);
        Market market = new Market(1L, "Winner", true, List.of(runner));
        Event expectedEvent = new Event(1L, "Match", System.currentTimeMillis(), List.of(market));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Event.class))
                .thenReturn(Mono.just(expectedEvent));

        // When & Then
        StepVerifier.create(apiService.getEventDetails(1L))
                .assertNext(event -> {
                    assertThat(event.name()).isEqualTo("Match");
                    assertThat(event.markets()).hasSize(1);
                    assertThat(event.markets().get(0).runners()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getEventDetails_onError_returnsEmpty() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Event.class))
                .thenReturn(Mono.error(new RuntimeException("Event not found")));

        // When & Then
        StepVerifier.create(apiService.getEventDetails(999L))
                .verifyComplete();
    }
}
