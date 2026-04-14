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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody OrderEvent orderEvent) {
        orderEvent.setOrderId(UUID.randomUUID().toString());
        orderEvent.setStatus("CREATED");

        orderProducer.sendOrder(orderEvent);

        return ResponseEntity.ok(Map.of(
                "message", "Order placed successfully",
                "orderId", orderEvent.getOrderId()
        ));
    }
}
