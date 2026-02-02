package com.example.demo;

import com.example.demo.service.LeonBetsParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Demo4Application {

    public static void main(String[] args) {
        SpringApplication.run(Demo4Application.class, args);
    }

    @Bean
    public CommandLineRunner run(LeonBetsParser parser) {
        return args -> {
            System.out.println("Starting Leon Bets Parser...");
            System.out.println("Parsing prematch data for: Football, Tennis, Ice Hockey, Basketball");
            System.out.println("=".repeat(70));

            parser.parse().block();

            System.out.println();
            System.out.println("=".repeat(70));
            System.out.println("Parsing completed!");
        };
    }
}
