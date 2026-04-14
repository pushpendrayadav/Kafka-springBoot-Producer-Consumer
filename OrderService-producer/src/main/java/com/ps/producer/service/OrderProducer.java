package com.ps.producer.service;

import com.ps.producer.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);
    private static final String TOPIC = "orders";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrder(OrderEvent orderEvent) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(TOPIC, orderEvent.getOrderId(), orderEvent);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent order [{}] to topic [{}] partition [{}] offset [{}]",
                        orderEvent.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order [{}]: {}", orderEvent.getOrderId(), ex.getMessage());
            }
        });
        log.info("Order [{}] sent to Kafka topic [{}]", orderEvent.getOrderId(), TOPIC);    
    }
}
