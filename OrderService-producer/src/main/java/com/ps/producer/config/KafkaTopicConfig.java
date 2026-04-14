package com.ps.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration class.
 * Automatically creates the required Kafka topics on application startup
 * if they do not already exist.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Creates the "orders" topic with 1 partition and 1 replica.
     * This topic is used by the OrderProducer to publish order events.
     */
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")  // Topic name
                .partitions(1)              // Number of partitions
                .replicas(1)                // Replication factor
                .build();
    }
}
