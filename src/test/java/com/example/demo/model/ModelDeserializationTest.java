package com.example.demo.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void sport_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 1,
                    "name": "Football",
                    "family": "Soccer",
                    "regions": [],
                    "unknownField": "ignored"
                }
                """;

        Sport sport = objectMapper.readValue(json, Sport.class);

        assertThat(sport.id()).isEqualTo(1L);
        assertThat(sport.name()).isEqualTo("Football");
        assertThat(sport.family()).isEqualTo("Soccer");
        assertThat(sport.regions()).isEmpty();
    }

    @Test
    void region_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 100,
                    "name": "England",
                    "leagues": []
                }
                """;

        Region region = objectMapper.readValue(json, Region.class);

        assertThat(region.id()).isEqualTo(100L);
        assertThat(region.name()).isEqualTo("England");
        assertThat(region.leagues()).isEmpty();
    }

    @Test
    void league_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 200,
                    "name": "Premier League",
                    "top": true,
                    "topOrder": 1,
                    "prematch": 15
                }
                """;

        League league = objectMapper.readValue(json, League.class);

        assertThat(league.id()).isEqualTo(200L);
        assertThat(league.name()).isEqualTo("Premier League");
        assertThat(league.top()).isTrue();
        assertThat(league.topOrder()).isEqualTo(1);
        assertThat(league.prematch()).isEqualTo(15);
    }

    @Test
    void event_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 1000,
                    "name": "Team A vs Team B",
                    "kickoff": 1704067200000,
                    "markets": []
                }
                """;

        Event event = objectMapper.readValue(json, Event.class);

        assertThat(event.id()).isEqualTo(1000L);
        assertThat(event.name()).isEqualTo("Team A vs Team B");
        assertThat(event.kickoff()).isEqualTo(1704067200000L);
        assertThat(event.markets()).isEmpty();
    }

    @Test
    void market_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 10,
                    "name": "Winner",
                    "open": true,
                    "runners": []
                }
                """;

        Market market = objectMapper.readValue(json, Market.class);

        assertThat(market.id()).isEqualTo(10L);
        assertThat(market.name()).isEqualTo("Winner");
        assertThat(market.open()).isTrue();
        assertThat(market.runners()).isEmpty();
    }

    @Test
    void runner_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 100,
                    "name": "Home Win",
                    "price": 1.95,
                    "open": true
                }
                """;

        Runner runner = objectMapper.readValue(json, Runner.class);

        assertThat(runner.id()).isEqualTo(100L);
        assertThat(runner.name()).isEqualTo("Home Win");
        assertThat(runner.price()).isEqualTo(1.95);
        assertThat(runner.open()).isTrue();
    }

    @Test
    void eventsResponse_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "events": [
                        {
                            "id": 1,
                            "name": "Match 1",
                            "kickoff": 1704067200000,
                            "markets": []
                        }
                    ]
                }
                """;

        EventsResponse response = objectMapper.readValue(json, EventsResponse.class);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).name()).isEqualTo("Match 1");
    }

    @Test
    void eventsResponse_withNullEvents_returnsEmptyList() throws Exception {
        String json = """
                {
                    "events": null
                }
                """;

        EventsResponse response = objectMapper.readValue(json, EventsResponse.class);

        assertThat(response.events()).isEmpty();
    }

    @Test
    void fullHierarchy_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 1,
                    "name": "Football",
                    "family": "Soccer",
                    "regions": [
                        {
                            "id": 100,
                            "name": "England",
                            "leagues": [
                                {
                                    "id": 200,
                                    "name": "Premier League",
                                    "top": true,
                                    "topOrder": 1,
                                    "prematch": 10
                                }
                            ]
                        }
                    ]
                }
                """;

        Sport sport = objectMapper.readValue(json, Sport.class);

        assertThat(sport.name()).isEqualTo("Football");
        assertThat(sport.regions()).hasSize(1);
        assertThat(sport.regions().get(0).name()).isEqualTo("England");
        assertThat(sport.regions().get(0).leagues()).hasSize(1);
        assertThat(sport.regions().get(0).leagues().get(0).name()).isEqualTo("Premier League");
    }

    @Test
    void eventWithMarkets_deserializesCorrectly() throws Exception {
        String json = """
                {
                    "id": 1000,
                    "name": "Team A vs Team B",
                    "kickoff": 1704067200000,
                    "markets": [
                        {
                            "id": 10,
                            "name": "Winner",
                            "open": true,
                            "runners": [
                                {"id": 1, "name": "1", "price": 1.95, "open": true},
                                {"id": 2, "name": "X", "price": 3.50, "open": true},
                                {"id": 3, "name": "2", "price": 4.20, "open": true}
                            ]
                        }
                    ]
                }
                """;

        Event event = objectMapper.readValue(json, Event.class);

        assertThat(event.markets()).hasSize(1);
        assertThat(event.markets().get(0).runners()).hasSize(3);
        assertThat(event.markets().get(0).runners().get(0).price()).isEqualTo(1.95);
        assertThat(event.markets().get(0).runners().get(1).price()).isEqualTo(3.50);
        assertThat(event.markets().get(0).runners().get(2).price()).isEqualTo(4.20);
    }
}
