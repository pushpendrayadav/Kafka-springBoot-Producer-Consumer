package com.ps.loadtest.config;

import com.ps.loadtest.dto.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * Kafka Producer Configuration for Load Testing
 * ============================================================
 *
 * This class creates FOUR different KafkaTemplate beans, each
 * configured with a different ACK strategy:
 *
 * 1. FIRE-AND-FORGET (acks=0, retries=0)
 *    - Producer does NOT wait for any broker acknowledgement.
 *    - Fastest, but messages WILL BE LOST if broker is down.
 *    - Use case: metrics, logs where loss is acceptable.
 *
 * 2. ACK=0 (acks=0, retries=3)
 *    - Same as fire-and-forget but with retries.
 *    - Retries are USELESS with acks=0 because the producer
 *      never knows if the send failed.
 *    - Included to demonstrate that retries don't help with acks=0.
 *
 * 3. ACK=1 (acks=1, retries=3)
 *    - Producer waits for the LEADER broker to acknowledge.
 *    - If the leader crashes BEFORE replicating, message is LOST.
 *    - Good balance between performance and reliability.
 *
 * 4. ACK=ALL (acks=all, retries=3, idempotent=true)
 *    - Producer waits for ALL in-sync replicas to acknowledge.
 *    - Combined with idempotence to prevent duplicate messages.
 *    - SAFEST option — no message loss even if leader crashes.
 *    - Slowest due to waiting for all replicas.
 *
 * ============================================================
 * WHAT HAPPENS WHEN KAFKA IS INTERMITTENTLY UNAVAILABLE?
 * ============================================================
 *
 * - acks=0:  Messages silently dropped. Producer has no idea.
 * - acks=1:  Producer gets an error; retries help if broker
 *            comes back within the retry window.
 * - acks=all: Same as acks=1 but safer; retries + idempotence
 *             ensure exactly-once delivery on recovery.
 *
 * MITIGATION STRATEGIES (demonstrated here):
 * - retries + retry.backoff.ms: Auto-retry on transient failures
 * - delivery.timeout.ms: Upper bound on total send time
 * - enable.idempotence=true: Prevents duplicates from retries
 * - max.in.flight.requests.per.connection=5: With idempotence,
 *   Kafka guarantees ordering even with 5 in-flight requests
 * ============================================================
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Topic auto-creation ──────────────────────────────────
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ── Common producer properties shared by all templates ───
    private Map<String, Object> commonProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    // ═══════════════════════════════════════════════════════════
    // 1. FIRE-AND-FORGET: acks=0, retries=0
    //    → Fastest. No acknowledgement. Messages lost if broker is down.
    // ═══════════════════════════════════════════════════════════
    @Bean
    public ProducerFactory<String, OrderEvent> fireAndForgetProducerFactory() {
        Map<String, Object> props = commonProps();
        props.put(ProducerConfig.ACKS_CONFIG, "0");        // Don't wait for any ACK
        props.put(ProducerConfig.RETRIES_CONFIG, 0);        // No retries
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("fireAndForgetKafkaTemplate")
    public KafkaTemplate<String, OrderEvent> fireAndForgetKafkaTemplate() {
        return new KafkaTemplate<>(fireAndForgetProducerFactory());
    }

    // ═══════════════════════════════════════════════════════════
    // 2. ACK=0 WITH RETRIES: acks=0, retries=3
    //    → Retries are pointless with acks=0 (producer never sees errors).
    //    → Included to prove retries don't help without acknowledgements.
    // ═══════════════════════════════════════════════════════════
    @Bean
    public ProducerFactory<String, OrderEvent> ack0ProducerFactory() {
        Map<String, Object> props = commonProps();
        props.put(ProducerConfig.ACKS_CONFIG, "0");        // No ACK
        props.put(ProducerConfig.RETRIES_CONFIG, 3);        // Retries (useless with acks=0)
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1s between retries
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("ack0KafkaTemplate")
    public KafkaTemplate<String, OrderEvent> ack0KafkaTemplate() {
        return new KafkaTemplate<>(ack0ProducerFactory());
    }

    // ═══════════════════════════════════════════════════════════
    // 3. ACK=1: acks=1, retries=3
    //    → Leader-only acknowledgement. Fast + reasonably safe.
    //    → Message can be lost if leader crashes before replicating.
    // ═══════════════════════════════════════════════════════════
    @Bean
    public ProducerFactory<String, OrderEvent> ack1ProducerFactory() {
        Map<String, Object> props = commonProps();
        props.put(ProducerConfig.ACKS_CONFIG, "1");         // Wait for leader ACK only
        props.put(ProducerConfig.RETRIES_CONFIG, 3);         // Retry up to 3 times
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1s between retries
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000); // 30s max total delivery time
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("ack1KafkaTemplate")
    public KafkaTemplate<String, OrderEvent> ack1KafkaTemplate() {
        return new KafkaTemplate<>(ack1ProducerFactory());
    }

    // ═══════════════════════════════════════════════════════════
    // 4. ACK=ALL: acks=all, retries=3, idempotent=true
    //    → ALL in-sync replicas must acknowledge.
    //    → Idempotence prevents duplicates from retries.
    //    → SAFEST configuration — no message loss.
    //    → Slowest due to replication wait.
    // ═══════════════════════════════════════════════════════════
    @Bean
    public ProducerFactory<String, OrderEvent> ackAllProducerFactory() {
        Map<String, Object> props = commonProps();
        props.put(ProducerConfig.ACKS_CONFIG, "all");       // Wait for ALL in-sync replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);         // Retry up to 3 times
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1s between retries
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicate messages
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000); // 30s max total delivery time
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // Safe with idempotence
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("ackAllKafkaTemplate")
    public KafkaTemplate<String, OrderEvent> ackAllKafkaTemplate() {
        return new KafkaTemplate<>(ackAllProducerFactory());
    }
}
