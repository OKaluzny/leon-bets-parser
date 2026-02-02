package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class LeonBetsParser {

    private static final Logger log = LoggerFactory.getLogger(LeonBetsParser.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final Duration RATE_LIMIT_DELAY = Duration.ofMillis(100);

    private final LeonApiService apiService;
    private final int maxParallelRequests;
    private final int matchesPerLeague;
    private final Set<String> targetSports;

    public LeonBetsParser(LeonApiService apiService, LeonApiProperties properties) {
        this.apiService = apiService;
        this.maxParallelRequests = properties.parser().maxParallelRequests();
        this.matchesPerLeague = properties.parser().matchesPerLeague();
        this.targetSports = Set.copyOf(properties.parser().targetSports());
    }

    public Mono<Void> parse() {
        log.info("Starting parser for sports: {}", targetSports);

        return apiService.getSports()
                .doOnNext(sports -> log.info("Fetched {} sports from API", sports.size()))
                .flatMapMany(Flux::fromIterable)
                .filter(sport -> targetSports.contains(sport.family()))
                .doOnNext(sport -> log.debug("Processing sport: {}", sport.name()))
                .flatMap(this::processTopLeagues, maxParallelRequests)
                .then()
                .doOnSuccess(v -> log.info("Parsing completed successfully"))
                .doOnError(e -> log.error("Parsing failed", e));
    }

    private Flux<Void> processTopLeagues(Sport sport) {
        if (sport.regions() == null) {
            log.debug("No regions found for sport: {}", sport.name());
            return Flux.empty();
        }

        List<LeagueContext> topLeagues = collectTopLeagues(sport);
        log.info("Found {} top leagues for {}", topLeagues.size(), sport.name());

        return Flux.fromIterable(topLeagues)
                .delayElements(RATE_LIMIT_DELAY)
                .flatMap(this::processLeague, maxParallelRequests);
    }

    private List<LeagueContext> collectTopLeagues(Sport sport) {
        List<LeagueContext> topLeagues = new ArrayList<>();

        for (Region region : sport.regions()) {
            if (region.leagues() == null) continue;

            for (League league : region.leagues()) {
                if (league.top() && league.prematch() > 0) {
                    topLeagues.add(new LeagueContext(sport, region, league));
                }
            }
        }

        topLeagues.sort(Comparator.comparingInt(ctx -> ctx.league().topOrder()));
        return topLeagues;
    }

    private Mono<Void> processLeague(LeagueContext ctx) {
        log.debug("Processing league: {} - {}", ctx.region().name(), ctx.league().name());

        return apiService.getEventsByLeague(ctx.league().id())
                .flatMapMany(response -> {
                    if (response.events() == null || response.events().isEmpty()) {
                        log.debug("No events found for league: {}", ctx.league().name());
                        return Flux.empty();
                    }
                    log.debug("Found {} events in league {}", response.events().size(), ctx.league().name());
                    return Flux.fromIterable(response.events()).take(matchesPerLeague);
                })
                .delayElements(RATE_LIMIT_DELAY)
                .flatMap(event -> processEvent(event, ctx), maxParallelRequests)
                .then();
    }

    private Mono<Void> processEvent(Event event, LeagueContext ctx) {
        return apiService.getEventDetails(event.id())
                .publishOn(Schedulers.single())
                .doOnNext(fullEvent -> printEvent(fullEvent, ctx))
                .then();
    }

    private void printEvent(Event event, LeagueContext ctx) {
        String leagueName = ctx.region().name() + " " + ctx.league().name();
        String kickoffStr = DATE_FORMATTER.format(Instant.ofEpochMilli(event.kickoff()));

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(ctx.sport().name()).append(", ").append(leagueName).append("\n");
        sb.append("    ").append(event.name()).append(", ").append(kickoffStr)
                .append(", ").append(event.id()).append("\n");

        if (event.markets() != null) {
            for (Market market : event.markets()) {
                if (!market.open()) continue;
                sb.append("        ").append(market.name()).append("\n");
                if (market.runners() != null) {
                    for (Runner runner : market.runners()) {
                        if (!runner.open()) continue;
                        sb.append("            ").append(runner.name())
                                .append(", ").append(runner.price())
                                .append(", ").append(runner.id()).append("\n");
                    }
                }
            }
        }

        System.out.print(sb);
        System.out.flush();
    }

    private record LeagueContext(Sport sport, Region region, League league) {}
}
