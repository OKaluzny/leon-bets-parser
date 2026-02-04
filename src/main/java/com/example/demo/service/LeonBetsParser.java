package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.Event;
import com.example.demo.model.League;
import com.example.demo.model.Market;
import com.example.demo.model.Region;
import com.example.demo.model.Runner;
import com.example.demo.model.Sport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@SuppressWarnings("PMD.SystemPrintln")
public class LeonBetsParser {

    private static final Logger LOG = LoggerFactory.getLogger(LeonBetsParser.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final int STRING_BUILDER_INITIAL_CAPACITY = 512;

    private final LeonApiService apiService;
    private final int maxParallelRequests;
    private final int matchesPerLeague;
    private final Set<String> targetSports;
    private final PrintStream outputStream;

    @Autowired
    public LeonBetsParser(LeonApiService apiService, LeonApiProperties properties) {
        this(apiService, properties, System.out);
    }

    LeonBetsParser(LeonApiService apiService, LeonApiProperties properties, PrintStream outputStream) {
        this.apiService = apiService;
        this.maxParallelRequests = properties.parser().maxParallelRequests();
        this.matchesPerLeague = properties.parser().matchesPerLeague();
        this.targetSports = Set.copyOf(properties.parser().targetSports());
        this.outputStream = outputStream;
    }

    public Mono<Void> parse() {
        LOG.info("Starting parser for sports: {}", targetSports);

        return apiService.getSports()
                .doOnNext(sports -> LOG.info("Fetched {} sports from API", sports.size()))
                .flatMapMany(Flux::fromIterable)
                .filter(sport -> targetSports.contains(sport.family()))
                .doOnNext(sport -> LOG.debug("Processing sport: {}", sport.name()))
                .concatMap(this::processTopLeagues)
                .then()
                .doOnSuccess(v -> LOG.info("Parsing completed successfully"))
                .doOnError(e -> LOG.error("Parsing failed", e));
    }

    private Flux<Void> processTopLeagues(Sport sport) {
        if (sport.regions() == null) {
            LOG.debug("No regions found for sport: {}", sport.name());
            return Flux.empty();
        }

        List<LeagueContext> topLeagues = collectTopLeagues(sport);
        LOG.info("Found {} top leagues for {}", topLeagues.size(), sport.name());

        return Flux.fromIterable(topLeagues)
                .flatMap(this::processLeague, maxParallelRequests);
    }

    private List<LeagueContext> collectTopLeagues(Sport sport) {
        List<LeagueContext> topLeagues = new ArrayList<>();

        for (Region region : sport.regions()) {
            if (region.leagues() == null) {
                continue;
            }

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
        return apiService.getEventsByLeague(ctx.league().id())
                .flatMapMany(response -> {
                    if (response.events() == null || response.events().isEmpty()) {
                        LOG.debug("No events found for league: {}", ctx.league().name());
                        return Flux.empty();
                    }
                    LOG.info("Processing league: {} - {} ({} events)",
                            ctx.region().name(), ctx.league().name(), response.events().size());
                    return Flux.fromIterable(response.events()).take(matchesPerLeague);
                })
                .concatMap(event -> processEvent(event, ctx))
                .then();
    }

    private Mono<Void> processEvent(Event event, LeagueContext ctx) {
        return apiService.getEventDetails(event.id())
                .doOnNext(fullEvent -> printEvent(fullEvent, ctx))
                .then();
    }

    private void printEvent(Event event, LeagueContext ctx) {
        StringBuilder sb = new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);

        appendEventHeader(sb, event, ctx);
        appendMarkets(sb, event);

        outputStream.print(sb);
        outputStream.flush();
    }

    private void appendEventHeader(StringBuilder sb, Event event, LeagueContext ctx) {
        String leagueName = ctx.region().name() + " " + ctx.league().name();
        String kickoffStr = DATE_FORMATTER.format(Instant.ofEpochMilli(event.kickoff()));

        sb.append('\n')
          .append(ctx.sport().name())
          .append(", ")
          .append(leagueName)
          .append("\n    ")
          .append(event.name())
          .append(", ")
          .append(kickoffStr)
          .append(", ")
          .append(event.id())
          .append('\n');
    }

    private void appendMarkets(StringBuilder sb, Event event) {
        if (event.markets() == null) {
            return;
        }

        for (Market market : event.markets()) {
            if (!market.open()) {
                continue;
            }
            sb.append("        ").append(market.name()).append('\n');
            appendRunners(sb, market);
        }
    }

    private void appendRunners(StringBuilder sb, Market market) {
        if (market.runners() == null) {
            return;
        }

        for (Runner runner : market.runners()) {
            if (!runner.open()) {
                continue;
            }
            sb.append("            ")
              .append(runner.name()).append(", ")
              .append(runner.price()).append(", ")
              .append(runner.id()).append('\n');
        }
    }

    private record LeagueContext(Sport sport, Region region, League league) { }
}
