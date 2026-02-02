package com.example.demo.service;

import com.example.demo.model.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LeonBetsParser {

    private static final int MAX_PARALLEL_REQUESTS = 3;
    private static final int MATCHES_PER_LEAGUE = 2;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final Set<String> TARGET_SPORTS = Set.of("Soccer", "Tennis", "IceHockey", "Basketball");

    private final LeonApiService apiService;

    public LeonBetsParser(LeonApiService apiService) {
        this.apiService = apiService;
    }

    public Mono<Void> parse() {
        return apiService.getSports()
                .flatMapMany(Flux::fromIterable)
                .filter(sport -> TARGET_SPORTS.contains(sport.getFamily()))
                .flatMap(this::processTopLeagues, MAX_PARALLEL_REQUESTS)
                .then();
    }

    private Flux<Void> processTopLeagues(Sport sport) {
        if (sport.getRegions() == null) return Flux.empty();

        List<LeagueWithSport> topLeagues = new ArrayList<>();
        for (Region region : sport.getRegions()) {
            if (region.getLeagues() == null) continue;
            for (League league : region.getLeagues()) {
                if (league.isTop() && league.getPrematch() > 0) {
                    league.setRegion(region);
                    topLeagues.add(new LeagueWithSport(league, sport, region));
                }
            }
        }

        topLeagues.sort(Comparator.comparingInt(l -> l.league().getTopOrder()));

        return Flux.fromIterable(topLeagues)
                .flatMap(this::processLeague, MAX_PARALLEL_REQUESTS);
    }

    private Mono<Void> processLeague(LeagueWithSport leagueWithSport) {
        League league = leagueWithSport.league();
        Sport sport = leagueWithSport.sport();
        Region region = leagueWithSport.region();

        return apiService.getEventsByLeague(league.getId())
                .flatMapMany(response -> {
                    if (response.getEvents() == null || response.getEvents().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.getEvents())
                            .take(MATCHES_PER_LEAGUE);
                })
                .flatMap(event -> processEvent(event, sport, league, region), MAX_PARALLEL_REQUESTS)
                .then();
    }

    private Mono<Void> processEvent(Event event, Sport sport, League league, Region region) {
        return apiService.getEventDetails(event.getId())
                .doOnNext(fullEvent -> printEvent(fullEvent, sport, league, region))
                .then();
    }

    private synchronized void printEvent(Event event, Sport sport, League league, Region region) {
        String leagueName = region.getName() + " " + league.getName();
        String kickoffStr = DATE_FORMATTER.format(Instant.ofEpochMilli(event.getKickoff()));

        System.out.println();
        System.out.println(sport.getName() + ", " + leagueName);
        System.out.println("    " + event.getName() + ", " + kickoffStr + ", " + event.getId());

        if (event.getMarkets() != null) {
            for (Market market : event.getMarkets()) {
                if (!market.isOpen()) continue;
                System.out.println("        " + market.getName());
                if (market.getRunners() != null) {
                    for (Runner runner : market.getRunners()) {
                        if (!runner.isOpen()) continue;
                        System.out.println("            " + runner.getName() + ", " + runner.getPrice() + ", " + runner.getId());
                    }
                }
            }
        }
        System.out.flush();
    }

    private record LeagueWithSport(League league, Sport sport, Region region) {}
}
