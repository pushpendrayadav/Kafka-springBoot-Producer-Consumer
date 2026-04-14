package com.ps.loadtest.controller;

import com.ps.loadtest.dto.OrderEvent;
import com.ps.loadtest.service.LoadTestProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ============================================================
 * Load Test REST Controller
 * ============================================================
 * Exposes endpoints for each ACK mode so K6 can target them
 * independently and compare throughput, latency, and message loss.
 *
 * Endpoints:
 *   POST /api/load-test/fire-and-forget  → acks=0, no callback
 *   POST /api/load-test/ack-0            → acks=0, with callback
 *   POST /api/load-test/ack-1            → acks=1
 *   POST /api/load-test/ack-all          → acks=all + idempotent
 *   GET  /api/load-test/stats            → Returns sent/success/failed counters
 *   POST /api/load-test/reset            → Resets counters for a new test run
 * ============================================================
 */
@RestController
@RequestMapping("/api/load-test")
public class LoadTestController {

    private final LoadTestProducer producer;

    public LoadTestController(LoadTestProducer producer) {
        this.producer = producer;
    }

    // ── FIRE-AND-FORGET ──────────────────────────────────────
    // send() is called but the result Future is ignored.
    // The HTTP response returns immediately.
    @PostMapping("/fire-and-forget")
    public ResponseEntity<Map<String, String>> fireAndForget(@RequestBody OrderEvent event) {
        event.setOrderId(UUID.randomUUID().toString());
        event.setStatus("CREATED");
        producer.sendFireAndForget(event);
        return ResponseEntity.ok(Map.of(
                "mode", "fire-and-forget",
                "orderId", event.getOrderId()
        ));
    }

    // ── ACK=0 ────────────────────────────────────────────────
    // acks=0 with retries (retries are ineffective with acks=0)
    @PostMapping("/ack-0")
    public ResponseEntity<Map<String, String>> ack0(@RequestBody OrderEvent event) {
        event.setOrderId(UUID.randomUUID().toString());
        event.setStatus("CREATED");
        producer.sendAck0(event);
        return ResponseEntity.ok(Map.of(
                "mode", "ack-0",
                "orderId", event.getOrderId()
        ));
    }

    // ── ACK=1 ────────────────────────────────────────────────
    // Leader-only acknowledgement with retries
    @PostMapping("/ack-1")
    public ResponseEntity<Map<String, String>> ack1(@RequestBody OrderEvent event) {
        event.setOrderId(UUID.randomUUID().toString());
        event.setStatus("CREATED");
        producer.sendAck1(event);
        return ResponseEntity.ok(Map.of(
                "mode", "ack-1",
                "orderId", event.getOrderId()
        ));
    }

    // ── ACK=ALL ──────────────────────────────────────────────
    // All replicas must acknowledge + idempotent producer
    @PostMapping("/ack-all")
    public ResponseEntity<Map<String, String>> ackAll(@RequestBody OrderEvent event) {
        event.setOrderId(UUID.randomUUID().toString());
        event.setStatus("CREATED");
        producer.sendAckAll(event);
        return ResponseEntity.ok(Map.of(
                "mode", "ack-all",
                "orderId", event.getOrderId()
        ));
    }

    // ── STATS ────────────────────────────────────────────────
    // Returns counters so you can compare expected vs actual delivery
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalSent", producer.getTotalSent(),
                "totalSuccess", producer.getTotalSuccess(),
                "totalFailed", producer.getTotalFailed()
        ));
    }

    // ── RESET ────────────────────────────────────────────────
    // Reset counters before starting a new test run
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        producer.resetCounters();
        return ResponseEntity.ok(Map.of("message", "Counters reset"));
    }
}
