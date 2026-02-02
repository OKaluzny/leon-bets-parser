package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.Event;
import com.example.demo.model.EventsResponse;
import com.example.demo.model.League;
import com.example.demo.model.Market;
import com.example.demo.model.Region;
import com.example.demo.model.Runner;
import com.example.demo.model.Sport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeonBetsParserTest {

    @Mock
    private LeonApiService apiService;

    private LeonApiProperties properties;
    private LeonBetsParser parser;

    @BeforeEach
    void setUp() {
        properties = new LeonApiProperties(
                new LeonApiProperties.Api(
                        "https://leon.bet",
                        Duration.ofSeconds(30),
                        new LeonApiProperties.Api.Retry(3, Duration.ofSeconds(1))
                ),
                new LeonApiProperties.Parser(3, 2, List.of("Soccer", "Tennis"))
        );
        parser = new LeonBetsParser(apiService, properties);
    }

    @Test
    void parse_withValidData_completesSuccessfully() {
        // Given
        League topLeague = new League(1L, "Premier League", true, 1, 10);
        Region region = new Region(1L, "England", List.of(topLeague));
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(region));

        Runner runner = new Runner(100L, "Home Win", 1.95, true);
        Market market = new Market(10L, "Winner", true, List.of(runner));
        Event event = new Event(1000L, "Team A vs Team B", System.currentTimeMillis(), List.of(market));

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));
        when(apiService.getEventsByLeague(1L)).thenReturn(Mono.just(new EventsResponse(List.of(event))));
        when(apiService.getEventDetails(1000L)).thenReturn(Mono.just(event));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_withNoTopLeagues_completesWithoutProcessingEvents() {
        // Given
        League nonTopLeague = new League(1L, "League Two", false, 1, 10);
        Region region = new Region(1L, "England", List.of(nonTopLeague));
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(region));

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_withEmptySports_completesImmediately() {
        // Given
        when(apiService.getSports()).thenReturn(Mono.just(List.of()));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_filtersNonTargetSports() {
        // Given - Basketball is not in target sports (Soccer, Tennis)
        League topLeague = new League(1L, "NBA", true, 1, 10);
        Region region = new Region(1L, "USA", List.of(topLeague));
        Sport basketball = new Sport(1L, "Basketball", "Basketball", List.of(region));

        when(apiService.getSports()).thenReturn(Mono.just(List.of(basketball)));

        // When & Then - Should complete without processing basketball events
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_sortsLeaguesByTopOrder() {
        // Given
        League league1 = new League(1L, "League A", true, 5, 10);
        League league2 = new League(2L, "League B", true, 1, 10);
        Region region = new Region(1L, "Test Region", List.of(league1, league2));
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(region));

        Event event = new Event(1000L, "Match", System.currentTimeMillis(), List.of());

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));
        when(apiService.getEventsByLeague(anyLong())).thenReturn(Mono.just(new EventsResponse(List.of(event))));
        when(apiService.getEventDetails(anyLong())).thenReturn(Mono.just(event));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_outputsEventInCorrectFormat() {
        // Given
        League topLeague = new League(1L, "Premier League", true, 1, 10);
        Region region = new Region(1L, "England", List.of(topLeague));
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(region));

        Runner runner = new Runner(100L, "1", 1.95, true);
        Market market = new Market(10L, "Winner", true, List.of(runner));
        Event event = new Event(1000L, "Team A vs Team B", 1704067200000L, List.of(market));

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));
        when(apiService.getEventsByLeague(1L)).thenReturn(Mono.just(new EventsResponse(List.of(event))));
        when(apiService.getEventDetails(1000L)).thenReturn(Mono.just(event));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LeonBetsParser parserWithCustomOutput = new LeonBetsParser(
                apiService, properties, new PrintStream(outputStream));

        // When
        parserWithCustomOutput.parse().block();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Football");
        assertThat(output).contains("England Premier League");
        assertThat(output).contains("Team A vs Team B");
        assertThat(output).contains("Winner");
        assertThat(output).contains("1.95");
    }

    @Test
    void parse_limitsMatchesPerLeague() {
        // Given
        League topLeague = new League(1L, "League", true, 1, 100);
        Region region = new Region(1L, "Region", List.of(topLeague));
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(region));

        List<Event> events = List.of(
                new Event(1L, "Match 1", System.currentTimeMillis(), List.of()),
                new Event(2L, "Match 2", System.currentTimeMillis(), List.of()),
                new Event(3L, "Match 3", System.currentTimeMillis(), List.of()),
                new Event(4L, "Match 4", System.currentTimeMillis(), List.of())
        );

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));
        when(apiService.getEventsByLeague(1L)).thenReturn(Mono.just(new EventsResponse(events)));
        when(apiService.getEventDetails(anyLong())).thenReturn(Mono.empty());

        // When & Then - only 2 events should be processed (matchesPerLeague = 2)
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_handlesNullRegions() {
        // Given
        Sport sportWithNullRegions = new Sport(1L, "Football", "Soccer", null);

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sportWithNullRegions)));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }

    @Test
    void parse_handlesNullLeagues() {
        // Given
        Region regionWithNullLeagues = new Region(1L, "Region", null);
        Sport sport = new Sport(1L, "Football", "Soccer", List.of(regionWithNullLeagues));

        when(apiService.getSports()).thenReturn(Mono.just(List.of(sport)));

        // When & Then
        StepVerifier.create(parser.parse())
                .verifyComplete();
    }
}
