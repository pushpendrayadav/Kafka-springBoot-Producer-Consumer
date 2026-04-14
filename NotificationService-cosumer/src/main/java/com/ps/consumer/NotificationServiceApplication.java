package com.ps.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Notification Service (Kafka Consumer) application.
 * @SpringBootApplication enables auto-configuration, component scanning, and configuration.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Bootstrap and launch the Spring Boot application
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
