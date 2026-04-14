/*
 * ============================================================
 * K6 Load Test: Compare ALL ACK Modes Side-by-Side
 * ============================================================
 * Runs all four producer modes in sequence so you can compare
 * throughput, latency, and message loss in a single test run.
 *
 * Run:  k6 run k6-scripts/compare-all-modes.js
 *
 * HOW TO SIMULATE KAFKA UNAVAILABILITY:
 *   While this test is running (during the "steady" phase),
 *   stop Kafka for 5–10 seconds, then restart it:
 *
 *     # Stop Kafka broker
 *     kafka-server-stop
 *
 *     # Wait 5-10 seconds...
 *
 *     # Restart Kafka broker
 *     kafka-server-start /opt/homebrew/etc/kafka/server.properties
 *
 *   Then check /api/load-test/stats after each scenario to see
 *   which mode lost messages and which recovered.
 *
 * EXPECTED RESULTS:
 *   ┌─────────────────────┬───────────┬───────────┬──────────────┐
 *   │ Mode                │ Speed     │ Msg Loss  │ On Recovery  │
 *   ├─────────────────────┼───────────┼───────────┼──────────────┤
 *   │ fire-and-forget     │ Fastest   │ YES       │ No retry     │
 *   │ ack=0 + retries     │ Fastest   │ YES       │ No retry(*)  │
 *   │ ack=1               │ Medium    │ Possible  │ Retries help │
 *   │ ack=all + idempotent│ Slowest   │ NO(**)    │ Retries help │
 *   └─────────────────────┴───────────┴───────────┴──────────────┘
 *   (*) Retries are useless with acks=0
 *   (**) As long as broker recovers within delivery.timeout.ms (30s)
 * ============================================================
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";

const BASE_URL = "http://localhost:8090";
const HEADERS = { "Content-Type": "application/json" };

// Counters per scenario
const fireForgetCount = new Counter("fire_forget_requests");
const ack0Count = new Counter("ack0_requests");
const ack1Count = new Counter("ack1_requests");
const ackAllCount = new Counter("ack_all_requests");

export const options = {
  scenarios: {
    // ── Scenario 1: Fire-and-Forget ──────────────────────
    fire_and_forget: {
      executor: "constant-vus",
      vus: 20,
      duration: "30s",
      exec: "fireAndForget",
      startTime: "0s", // Start immediately
    },
    // Reset counters between scenarios
    reset_after_ff: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "resetAndPrintStats",
      startTime: "32s", // After fire-and-forget ends
      env: { SCENARIO_NAME: "FIRE-AND-FORGET" },
    },
    // ── Scenario 2: ACK=0 ────────────────────────────────
    ack_0: {
      executor: "constant-vus",
      vus: 20,
      duration: "30s",
      exec: "ack0",
      startTime: "35s",
    },
    reset_after_ack0: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "resetAndPrintStats",
      startTime: "67s",
      env: { SCENARIO_NAME: "ACK-0" },
    },
    // ── Scenario 3: ACK=1 ────────────────────────────────
    ack_1: {
      executor: "constant-vus",
      vus: 20,
      duration: "30s",
      exec: "ack1",
      startTime: "70s",
    },
    reset_after_ack1: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "resetAndPrintStats",
      startTime: "102s",
      env: { SCENARIO_NAME: "ACK-1" },
    },
    // ── Scenario 4: ACK=ALL ──────────────────────────────
    ack_all: {
      executor: "constant-vus",
      vus: 20,
      duration: "30s",
      exec: "ackAll",
      startTime: "105s",
    },
    reset_after_ack_all: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "resetAndPrintStats",
      startTime: "137s",
      env: { SCENARIO_NAME: "ACK-ALL" },
    },
  },
};

function makePayload() {
  return JSON.stringify({
    productName: `Product-${__VU}-${__ITER}`,
    quantity: Math.floor(Math.random() * 10) + 1,
    price: parseFloat((Math.random() * 1000).toFixed(2)),
  });
}

// ── Scenario functions ──────────────────────────────────────

export function fireAndForget() {
  const res = http.post(
    `${BASE_URL}/api/load-test/fire-and-forget`,
    makePayload(),
    { headers: HEADERS },
  );
  check(res, { "status 200": (r) => r.status === 200 });
  fireForgetCount.add(1);
  sleep(0.05);
}

export function ack0() {
  const res = http.post(`${BASE_URL}/api/load-test/ack-0`, makePayload(), {
    headers: HEADERS,
  });
  check(res, { "status 200": (r) => r.status === 200 });
  ack0Count.add(1);
  sleep(0.05);
}

export function ack1() {
  const res = http.post(`${BASE_URL}/api/load-test/ack-1`, makePayload(), {
    headers: HEADERS,
  });
  check(res, { "status 200": (r) => r.status === 200 });
  ack1Count.add(1);
  sleep(0.05);
}

export function ackAll() {
  const res = http.post(`${BASE_URL}/api/load-test/ack-all`, makePayload(), {
    headers: HEADERS,
  });
  check(res, { "status 200": (r) => r.status === 200 });
  ackAllCount.add(1);
  sleep(0.05);
}

// Print stats from the app and reset counters for the next scenario
export function resetAndPrintStats() {
  const scenarioName = __ENV.SCENARIO_NAME || "UNKNOWN";
  const statsRes = http.get(`${BASE_URL}/api/load-test/stats`);
  console.log(`\n=== ${scenarioName} Results ===`);
  console.log(`Stats: ${statsRes.body}`);
  console.log(`===============================\n`);
  http.post(`${BASE_URL}/api/load-test/reset`);
}
