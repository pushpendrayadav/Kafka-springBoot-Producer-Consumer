package com.ps.loadtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Kafka Producer Load Test application.
 * This app provides multiple REST endpoints, each configured with
 * different Kafka producer ACK strategies to observe behaviour
 * under load and during Kafka broker unavailability.
 */
@SpringBootApplication
public class LoadTestProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadTestProducerApplication.class, args);
    }
}
