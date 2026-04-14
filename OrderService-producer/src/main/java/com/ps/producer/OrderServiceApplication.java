package com.ps.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Order Service (Kafka Producer) application.
 * @SpringBootApplication enables auto-configuration, component scanning, and configuration.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        // Bootstrap and launch the Spring Boot application
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
