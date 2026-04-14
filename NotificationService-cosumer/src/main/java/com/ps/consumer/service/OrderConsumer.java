package com.ps.consumer.service;

import com.ps.consumer.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer service that listens to the "orders" topic
 * and processes incoming order events as notifications.
 */
@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    /**
     * Listens to the "orders" Kafka topic with consumer group "notification-group".
     * Each received OrderEvent is logged and a notification is simulated.
     *
     * @param orderEvent the deserialized order event from Kafka
     */
    @KafkaListener(topics = "orders", groupId = "notification-group")
    public void consume(OrderEvent orderEvent) {
        // Log the received order event details
        log.info("****** Received order event ******");
        log.info("Order ID    : {}", orderEvent.getOrderId());
        log.info("Product     : {}", orderEvent.getProductName());
        log.info("Quantity    : {}", orderEvent.getQuantity());
        log.info("Price       : {}", orderEvent.getPrice());
        log.info("Status      : {}", orderEvent.getStatus());
        log.info("*********************************");

        // Simulate sending a notification (e.g., email, SMS, push)
        log.info("Notification sent for order: {}", orderEvent.getOrderId());
    }
}
