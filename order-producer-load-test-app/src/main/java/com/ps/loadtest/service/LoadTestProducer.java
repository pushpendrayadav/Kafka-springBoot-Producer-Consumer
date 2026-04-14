package com.ps.loadtest.service;

import com.ps.loadtest.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Producer service that exposes different send strategies.
 *
 * Maintains atomic counters for sent/failed/total messages
 * so you can query /api/load-test/stats to see how many
 * messages were lost under different configurations.
 */
@Service
public class LoadTestProducer {

    private static final Logger log = LoggerFactory.getLogger(LoadTestProducer.class);
    private static final String TOPIC = "orders";

    // ── Counters to track message delivery success/failure ───
    private final AtomicLong totalSent = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);

    // ── Four KafkaTemplates, each with a different ACK config ─
    private final KafkaTemplate<String, OrderEvent> fireAndForgetTemplate;
    private final KafkaTemplate<String, OrderEvent> ack0Template;
    private final KafkaTemplate<String, OrderEvent> ack1Template;
    private final KafkaTemplate<String, OrderEvent> ackAllTemplate;

    public LoadTestProducer(
            @Qualifier("fireAndForgetKafkaTemplate") KafkaTemplate<String, OrderEvent> fireAndForgetTemplate,
            @Qualifier("ack0KafkaTemplate") KafkaTemplate<String, OrderEvent> ack0Template,
            @Qualifier("ack1KafkaTemplate") KafkaTemplate<String, OrderEvent> ack1Template,
            @Qualifier("ackAllKafkaTemplate") KafkaTemplate<String, OrderEvent> ackAllTemplate) {
        this.fireAndForgetTemplate = fireAndForgetTemplate;
        this.ack0Template = ack0Template;
        this.ack1Template = ack1Template;
        this.ackAllTemplate = ackAllTemplate;
    }

    // ═══════════════════════════════════════════════════════════
    // FIRE-AND-FORGET
    // → Just call send() and don't attach any callback.
    // → The producer has NO IDEA if the message was delivered.
    // → If Kafka is down, the message is silently lost.
    // ═══════════════════════════════════════════════════════════
    public void sendFireAndForget(OrderEvent event) {
        totalSent.incrementAndGet();
        // send() returns a Future but we intentionally ignore it
        fireAndForgetTemplate.send(TOPIC, event.getOrderId(), event);
        // We optimistically count success (we can't know if it failed)
        totalSuccess.incrementAndGet();
        log.debug("[FIRE-AND-FORGET] Sent orderId={}", event.getOrderId());
    }

    // ═══════════════════════════════════════════════════════════
    // ACK=0 (with retries — but retries are useless here)
    // → Same behaviour as fire-and-forget because the producer
    //   never receives an error response with acks=0.
    // → Demonstrates that retries DON'T help without acks.
    // ═══════════════════════════════════════════════════════════
    public void sendAck0(OrderEvent event) {
        totalSent.incrementAndGet();
        CompletableFuture<SendResult<String, OrderEvent>> future =
                ack0Template.send(TOPIC, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                totalSuccess.incrementAndGet();
                log.debug("[ACK-0] Success orderId={} partition={} offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                totalFailed.incrementAndGet();
                log.error("[ACK-0] FAILED orderId={} error={}", event.getOrderId(), ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    // ACK=1 (leader acknowledgement)
    // → Producer waits for leader to confirm write.
    // → If Kafka is intermittently down, retries may save the message.
    // → If leader crashes AFTER ack but BEFORE replication → message lost.
    // ═══════════════════════════════════════════════════════════
    public void sendAck1(OrderEvent event) {
        totalSent.incrementAndGet();
        CompletableFuture<SendResult<String, OrderEvent>> future =
                ack1Template.send(TOPIC, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                totalSuccess.incrementAndGet();
                log.debug("[ACK-1] Success orderId={} partition={} offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                totalFailed.incrementAndGet();
                log.error("[ACK-1] FAILED orderId={} error={}", event.getOrderId(), ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    // ACK=ALL (all in-sync replicas + idempotence)
    // → SAFEST mode. Waits for all replicas to acknowledge.
    // → Idempotence ensures no duplicates even on retry.
    // → If Kafka is down, retries will keep trying for up to
    //   delivery.timeout.ms (30s). If broker recovers in time,
    //   the message is delivered successfully.
    // → If broker doesn't recover within 30s → error callback fires.
    // ═══════════════════════════════════════════════════════════
    public void sendAckAll(OrderEvent event) {
        totalSent.incrementAndGet();
        CompletableFuture<SendResult<String, OrderEvent>> future =
                ackAllTemplate.send(TOPIC, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                totalSuccess.incrementAndGet();
                log.debug("[ACK-ALL] Success orderId={} partition={} offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                totalFailed.incrementAndGet();
                log.error("[ACK-ALL] FAILED orderId={} error={}", event.getOrderId(), ex.getMessage());
            }
        });
    }

    // ── Stats getters for the /stats endpoint ────────────────
    public long getTotalSent() { return totalSent.get(); }
    public long getTotalSuccess() { return totalSuccess.get(); }
    public long getTotalFailed() { return totalFailed.get(); }

    /** Resets all counters to zero (call before starting a new test run) */
    public void resetCounters() {
        totalSent.set(0);
        totalSuccess.set(0);
        totalFailed.set(0);
    }
}
