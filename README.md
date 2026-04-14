# Kafka Spring Boot - Producer & Consumer

A simple event-driven microservices project demonstrating Kafka-based communication between two Spring Boot applications.

## Architecture

```
┌──────────────────────┐         ┌───────────┐         ┌─────────────────────────────┐
│  OrderService        │         │   Kafka   │         │  NotificationService        │
│  (Producer)          │────────▶│  Topic:   │────────▶│  (Consumer)                 │
│  POST /api/orders    │  send   │ "orders"  │ consume │  Logs order & sends notif.  │
│  Port: 8082          │         │           │         │  Port: 8084                 │
└──────────────────────┘         └───────────┘         └─────────────────────────────┘
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Kafka**
- **Apache Kafka** (broker required at `localhost:9092`)
- **Maven** (build tool)

## Project Structure

```
Kafka-springBoot-Producer-Consumer/
├── OrderService-producer/               # Kafka Producer Application
│   ├── pom.xml
│   └── src/main/java/com/ps/producer/
│       ├── OrderServiceApplication.java # Main class
│       ├── config/
│       │   └── KafkaTopicConfig.java    # Auto-creates "orders" topic
│       ├── controller/
│       │   └── OrderController.java     # REST API: POST /api/orders
│       ├── dto/
│       │   └── OrderEvent.java          # Order event DTO
│       └── service/
│           └── OrderProducer.java       # Publishes events to Kafka
│
├── NotificationService-cosumer/         # Kafka Consumer Application
│   ├── pom.xml
│   └── src/main/java/com/ps/consumer/
│       ├── NotificationServiceApplication.java  # Main class
│       ├── dto/
│       │   └── OrderEvent.java          # Order event DTO (for deserialization)
│       └── service/
│           └── OrderConsumer.java       # Consumes events from "orders" topic
│
├── .gitignore
└── README.md
```

## Prerequisites

- **Java 17+** installed
- **Apache Kafka** running on `localhost:9092`
- **Maven 3.8+** installed

### Start Kafka (using Homebrew on macOS)

```bash
# Start Zookeeper
zookeeper-server-start /opt/homebrew/etc/kafka/zookeeper.properties

# Start Kafka Broker
kafka-server-start /opt/homebrew/etc/kafka/server.properties
```

## How to Run

### 1. Start the Producer (OrderService)

```bash
cd OrderService-producer
mvn spring-boot:run
```

Runs on **port 8082**.

### 2. Start the Consumer (NotificationService)

```bash
cd NotificationService-cosumer
mvn spring-boot:run
```

Runs on **port 8084**.

### 3. Place an Order (send a Kafka event)

```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "iPhone 16",
    "quantity": 1,
    "price": 999.99
  }'
```

**Response:**

```json
{
  "message": "Order placed successfully",
  "orderId": "a1b2c3d4-e5f6-..."
}
```

### 4. Check Consumer Logs

The NotificationService console will show:

```
****** Received order event ******
Order ID    : a1b2c3d4-e5f6-...
Product     : iPhone 16
Quantity    : 1
Price       : 999.99
Status      : CREATED
*********************************
Notification sent for order: a1b2c3d4-e5f6-...
```

## API Reference

| Method | Endpoint      | Description       | Request Body                                              |
| ------ | ------------- | ----------------- | --------------------------------------------------------- |
| POST   | `/api/orders` | Place a new order | `{ "productName": "...", "quantity": 1, "price": 99.99 }` |

## Configuration

| Property                        | Producer (8082)    | Consumer (8084)      |
| ------------------------------- | ------------------ | -------------------- |
| `bootstrap-servers`             | `localhost:9092`   | `localhost:9092`     |
| `key-serializer/deserializer`   | `StringSerializer` | `StringDeserializer` |
| `value-serializer/deserializer` | `JsonSerializer`   | `JsonDeserializer`   |
| `consumer.group-id`             | —                  | `notification-group` |
| `auto-offset-reset`             | —                  | `earliest`           |
