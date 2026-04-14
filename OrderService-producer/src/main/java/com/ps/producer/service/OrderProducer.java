package com.ps.producer.service;

import com.ps.producer.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service responsible for sending order events
 * to the "orders" Kafka topic.
 */
@Service
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);
    private static final String TOPIC = "orders"; // Target Kafka topic name

    // KafkaTemplate is the high-level API to send messages to Kafka
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    // Constructor injection of KafkaTemplate (auto-configured by Spring Boot)
    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends an OrderEvent to the Kafka "orders" topic.
     * Uses the orderId as the message key for partition routing.
     *
     * @param orderEvent the order event to publish
     */
    public void sendOrder(OrderEvent orderEvent) {
        // Send the order event asynchronously; orderId is used as the Kafka message key
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(TOPIC, orderEvent.getOrderId(), orderEvent);

        // Register a callback to handle success or failure of the send operation
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Log success with topic, partition, and offset details
                log.info("Sent order [{}] to topic [{}] partition [{}] offset [{}]",
                        orderEvent.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Log failure with error message
                log.error("Failed to send order [{}]: {}", orderEvent.getOrderId(), ex.getMessage());
            }
        });
        log.info("Order [{}] sent to Kafka topic [{}]", orderEvent.getOrderId(), TOPIC);    
    }
}
