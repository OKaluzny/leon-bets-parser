package com.example.demo;

import com.example.demo.service.LeonBetsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class LeonBetsParserApplication {

    private static final Logger log = LoggerFactory.getLogger(LeonBetsParserApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LeonBetsParserApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(LeonBetsParser parser) {
        return args -> {
            log.info("Starting Leon Bets Parser...");
            System.out.println("=".repeat(70));
            System.out.println("Leon Bets Prematch Parser");
            System.out.println("Sports: Football, Tennis, Ice Hockey, Basketball");
            System.out.println("=".repeat(70));

            Instant start = Instant.now();

            parser.parse()
                    .doFinally(signal -> {
                        Duration elapsed = Duration.between(start, Instant.now());
                        System.out.println();
                        System.out.println("=".repeat(70));
                        log.info("Parsing finished in {} seconds", elapsed.toSeconds());
                        System.out.println("Completed in " + elapsed.toSeconds() + " seconds");
                    })
                    .block();
        };
    }
}
