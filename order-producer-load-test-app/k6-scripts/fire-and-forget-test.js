/*
 * ============================================================
 * K6 Load Test: Fire-and-Forget Producer
 * ============================================================
 * Tests the fire-and-forget endpoint (acks=0, no callback).
 *
 * Run:  k6 run k6-scripts/fire-and-forget-test.js
 *
 * Behaviour when Kafka is down:
 *   - HTTP 200 keeps returning (producer doesn't know Kafka is down)
 *   - Messages are silently LOST
 *   - Check /api/load-test/stats after the test to see the gap
 * ============================================================
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// Custom metrics
const ordersSent = new Counter("orders_sent");
const orderLatency = new Trend("order_latency");

// Load test configuration
export const options = {
  stages: [
    { duration: "10s", target: 10 }, // Ramp up to 10 virtual users
    { duration: "30s", target: 10 }, // Hold at 10 VUs for 30s
    { duration: "10s", target: 50 }, // Spike to 50 VUs
    { duration: "20s", target: 50 }, // Hold spike
    { duration: "10s", target: 0 }, // Ramp down
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"], // 95% of requests under 500ms
    http_req_failed: ["rate<0.01"], // Less than 1% HTTP errors
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

  const res = http.post(`${BASE_URL}/api/load-test/fire-and-forget`, payload, {
    headers: HEADERS,
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "has orderId": (r) => JSON.parse(r.body).orderId !== undefined,
  });

  ordersSent.add(1);
  orderLatency.add(res.timings.duration);

  sleep(0.1); // 100ms pause between requests per VU
}

// Print stats after test completes
export function handleSummary(data) {
  // Fetch stats from the app
  const res = http.get(`${BASE_URL}/api/load-test/stats`);
  console.log("\n=== FIRE-AND-FORGET Producer Stats ===");
  console.log(`App Stats: ${res.body}`);
  console.log("=====================================\n");
  return {};
}
