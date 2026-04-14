package com.ps.producer.controller;

import com.ps.producer.dto.OrderEvent;
import com.ps.producer.service.OrderProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller that exposes an API endpoint for placing orders.
 * Receives order requests via HTTP POST and publishes them to Kafka.
 */
@RestController
@RequestMapping("/api/orders") // Base path for all order-related endpoints
public class OrderController {

    // Injected producer service to send order events to Kafka
    private final OrderProducer orderProducer;

    // Constructor injection of OrderProducer
    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    /**
     * POST /api/orders — Places a new order.
     * Generates a unique orderId, sets status to CREATED,
     * and publishes the order event to Kafka.
     *
     * @param orderEvent the order details from the request body
     * @return response with success message and generated orderId
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody OrderEvent orderEvent) {
        // Generate a unique order ID using UUID
        orderEvent.setOrderId(UUID.randomUUID().toString());
        // Set initial order status
        orderEvent.setStatus("CREATED");

        // Publish the order event to Kafka topic
        orderProducer.sendOrder(orderEvent);

        // Return success response with the generated orderId
        return ResponseEntity.ok(Map.of(
                "message", "Order placed successfully",
                "orderId", orderEvent.getOrderId()
        ));
    }
}
