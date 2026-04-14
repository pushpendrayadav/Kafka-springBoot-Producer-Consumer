/*
 * ============================================================
 * K6 Load Test: ACK=0 Producer
 * ============================================================
 * Tests the ack-0 endpoint (acks=0, retries=3).
 *
 * Run:  k6 run k6-scripts/ack0-test.js
 *
 * Key insight:
 *   - Even though retries=3 is configured, with acks=0 the
 *     producer never receives an error, so retries never trigger.
 *   - Behaviour is nearly identical to fire-and-forget.
 *   - Compare stats with fire-and-forget to see they're the same.
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
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
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

  const res = http.post(`${BASE_URL}/api/load-test/ack-0`, payload, {
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
  console.log("\n=== ACK-0 Producer Stats ===");
  console.log(`App Stats: ${res.body}`);
  console.log("============================\n");
  return {};
}
