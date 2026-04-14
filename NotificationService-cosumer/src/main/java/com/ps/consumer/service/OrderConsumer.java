package com.ps.consumer.service;

import com.ps.consumer.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(topics = "orders", groupId = "notification-group")
    public void consume(OrderEvent orderEvent) {
        log.info("****** Received order event ******");
        log.info("Order ID    : {}", orderEvent.getOrderId());
        log.info("Product     : {}", orderEvent.getProductName());
        log.info("Quantity    : {}", orderEvent.getQuantity());
        log.info("Price       : {}", orderEvent.getPrice());
        log.info("Status      : {}", orderEvent.getStatus());
        log.info("*********************************");

        // Simulate sending a notification
        log.info("Notification sent for order: {}", orderEvent.getOrderId());
    }
}
