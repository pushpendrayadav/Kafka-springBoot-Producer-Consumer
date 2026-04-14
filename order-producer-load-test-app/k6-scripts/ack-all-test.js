/*
 * ============================================================
 * K6 Load Test: ACK=ALL Producer (Safest Mode)
 * ============================================================
 * Tests the ack-all endpoint (acks=all, retries=3, idempotent).
 *
 * Run:  k6 run k6-scripts/ack-all-test.js
 *
 * Behaviour when Kafka is intermittently down:
 *   - Producer detects failure immediately.
 *   - Retries with idempotent key → no duplicate messages.
 *   - If broker recovers within 30s → message delivered safely.
 *   - If broker stays down → totalFailed counter increments.
 *   - This is the SLOWEST mode but guarantees no message loss
 *     as long as brokers recover within the delivery timeout.
 *
 * Compare totalFailed between ack-1 and ack-all to see
 * how idempotence + all-replica ACKs improve reliability.
 * ============================================================
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

const ordersSent = new Counter("orders_sent");
const orderLatency = new Trend("order_latency");

export const options = {
  stages: [
    { duration: "10s", target: 10 },
    { duration: "30s", target: 10 },
    { duration: "10s", target: 50 },
    { duration: "20s", target: 50 },
    { duration: "10s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<2000"], // Higher threshold (acks=all is slowest)
    http_req_failed: ["rate<0.05"],
  },
};

const BASE_URL = "http://localhost:8090";
const HEADERS = { "Content-Type": "application/json" };

export default function () {
  const payload = JSON.stringify({
    productName: `Product-${__VU}-${__ITER}`,
    quantity: Math.floor(Math.random() * 10) + 1,
    price: parseFloat((Math.random() * 1000).toFixed(2)),
  });

  const res = http.post(`${BASE_URL}/api/load-test/ack-all`, payload, {
    headers: HEADERS,
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "has orderId": (r) => JSON.parse(r.body).orderId !== undefined,
  });

  ordersSent.add(1);
  orderLatency.add(res.timings.duration);

  sleep(0.1);
}

export function handleSummary(data) {
  const res = http.get(`${BASE_URL}/api/load-test/stats`);
  console.log("\n=== ACK-ALL Producer Stats ===");
  console.log(`App Stats: ${res.body}`);
  console.log("==============================\n");
  return {};
}
