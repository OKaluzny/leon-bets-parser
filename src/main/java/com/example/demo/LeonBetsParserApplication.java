package com.example.demo;

import com.example.demo.service.LeonBetsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
@SuppressWarnings("PMD.SystemPrintln")
public class LeonBetsParserApplication {

    private static final Logger LOG = LoggerFactory.getLogger(LeonBetsParserApplication.class);
    private static final int SEPARATOR_LENGTH = 70;

    private static final PrintStream OUT = System.out;

    public static void main(String[] args) {
        SpringApplication.run(LeonBetsParserApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(LeonBetsParser parser) {
        return args -> {
            LOG.info("Starting Leon Bets Parser...");
            printHeader();

            Instant start = Instant.now();

            parser.parse()
                    .doFinally(signal -> printFooter(start))
                    .block();
        };
    }

    private void printHeader() {
        OUT.println("=".repeat(SEPARATOR_LENGTH));
        OUT.println("Leon Bets Prematch Parser");
        OUT.println("Sports: Football, Tennis, Ice Hockey, Basketball");
        OUT.println("=".repeat(SEPARATOR_LENGTH));
    }

    private void printFooter(Instant start) {
        Duration elapsed = Duration.between(start, Instant.now());
        OUT.println();
        OUT.println("=".repeat(SEPARATOR_LENGTH));
        LOG.info("Parsing finished in {} seconds", elapsed.toSeconds());
        OUT.println("Completed in " + elapsed.toSeconds() + " seconds");
    }
}
