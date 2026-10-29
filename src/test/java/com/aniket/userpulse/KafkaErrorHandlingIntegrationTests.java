package com.aniket.userpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = { "user-activity", "user-activity.DLT" },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "userpulse.kafka.retry.backoff-ms=100",
        "userpulse.kafka.retry.max-attempts=2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(KafkaErrorHandlingIntegrationTests.TestRetryConfig.class)
class KafkaErrorHandlingIntegrationTests {

    private static final String TOPIC = "user-activity";
    private static final String DLT_TOPIC = "user-activity.DLT";

    @TestConfiguration
    static class TestRetryConfig {
        static final Map<String, List<Integer>> deliveryAttemptsByKey = new ConcurrentHashMap<>();
        static final List<ConsumerRecord<?, ?>> recoveredRecords = new CopyOnWriteArrayList<>();

        @Bean
        RetryListener testRetryListener() {
            return new RetryListener() {
                @Override
                public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                    if (record != null && record.key() != null) {
                        deliveryAttemptsByKey
                                .computeIfAbsent(record.key().toString(), k -> new CopyOnWriteArrayList<>())
                                .add(deliveryAttempt);
                    }
                }

                @Override
                public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                    recoveredRecords.add(record);
                }
            };
        }
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private ActivityAnalyticsService analyticsService;

    @Autowired
    private UserActivityProducer userActivityProducer;

    private Producer<String, String> rawStringProducer;
    private Consumer<String, byte[]> dltConsumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void setUpAll() {
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        }

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        rawStringProducer = new KafkaProducer<>(producerProps);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                embeddedKafkaBroker,
                "test-dlt-consumer-group",
                false
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        dltConsumer = new DefaultKafkaConsumerFactory<String, byte[]>(consumerProps).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(dltConsumer, DLT_TOPIC);
    }

    @AfterAll
    void tearDownAll() {
        if (rawStringProducer != null) {
            rawStringProducer.close(Duration.ofSeconds(5));
        }
        if (dltConsumer != null) {
            dltConsumer.close(Duration.ofSeconds(5));
        }
    }

    private ConsumerRecord<String, byte[]> pollForDltRecord(String expectedKey, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, byte[]> records = dltConsumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, byte[]> record : records) {
                if (expectedKey.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("Timed out waiting for DLT record with key: " + expectedKey);
    }

    @Test
    void shouldConsumeValidEventAndUpdateAnalytics() {
        String userId = "user-valid-" + UUID.randomUUID();
        UserActivityEvent event = new UserActivityEvent(userId, "LOGIN_SUCCESS");

        userActivityProducer.sendEvent(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Map<String, Long> counts = analyticsService.getCounts();
            assertThat(counts).containsKey("LOGIN_SUCCESS");
            assertThat(counts.get("LOGIN_SUCCESS")).isGreaterThanOrEqualTo(1L);
        });

        assertThat(TestRetryConfig.deliveryAttemptsByKey.get(userId)).isNull();
    }

    @Test
    void shouldHandleMalformedJsonAndRouteToDltAsRawBytes() {
        String malformedJson = "{ \"userId\": \"bad-json\", \"eventType\": ";
        String key = "malformed-key-" + UUID.randomUUID();
        int partition = 1;

        rawStringProducer.send(new ProducerRecord<>(TOPIC, partition, key, malformedJson));
        rawStringProducer.flush();

        ConsumerRecord<String, byte[]> dltRecord = pollForDltRecord(key, Duration.ofSeconds(10));

        assertThat(dltRecord).isNotNull();
        assertThat(dltRecord.key()).isEqualTo(key);
        assertThat(dltRecord.partition()).isEqualTo(partition);

        String recoveredValue = new String(dltRecord.value(), StandardCharsets.UTF_8);
        assertThat(recoveredValue).isEqualTo(malformedJson);

        Header originalTopicHeader = dltRecord.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        assertThat(originalTopicHeader).isNotNull();
        assertThat(new String(originalTopicHeader.value(), StandardCharsets.UTF_8)).isEqualTo(TOPIC);

        Header exceptionFqcnHeader = dltRecord.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
        assertThat(exceptionFqcnHeader).isNotNull();
        assertThat(new String(exceptionFqcnHeader.value(), StandardCharsets.UTF_8))
                .contains("DeserializationException");

        // Deserialization exceptions are fatal; only initial failed attempt (no retries)
        assertThat(TestRetryConfig.deliveryAttemptsByKey.get(key)).containsExactly(1);
    }

    @Test
    void shouldRetryListenerExceptionAccordingToPolicyAndRouteToDlt() throws Exception {
        String key = "listener-fail-" + UUID.randomUUID();
        int partition = 2;
        String jsonPayload = """
                {
                    "userId": "%s",
                    "eventType": "   "
                }
                """.formatted(key);

        rawStringProducer.send(new ProducerRecord<>(TOPIC, partition, key, jsonPayload));
        rawStringProducer.flush();

        ConsumerRecord<String, byte[]> dltRecord = pollForDltRecord(key, Duration.ofSeconds(10));

        assertThat(dltRecord).isNotNull();
        assertThat(dltRecord.key()).isEqualTo(key);
        assertThat(dltRecord.partition()).isEqualTo(partition);

        UserActivityEvent deserialized = objectMapper.readValue(dltRecord.value(), UserActivityEvent.class);
        assertThat(deserialized.userId()).isEqualTo(key);
        assertThat(deserialized.eventType()).isEqualTo("   ");

        Header exceptionFqcn = dltRecord.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
        assertThat(exceptionFqcn).isNotNull();
        assertThat(new String(exceptionFqcn.value(), StandardCharsets.UTF_8))
                .contains("ListenerExecutionFailedException");

        Header exceptionCauseFqcn = dltRecord.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN);
        assertThat(exceptionCauseFqcn).isNotNull();
        assertThat(new String(exceptionCauseFqcn.value(), StandardCharsets.UTF_8))
                .contains("IllegalArgumentException");

        Header exceptionMessage = dltRecord.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        assertThat(exceptionMessage).isNotNull();
        assertThat(new String(exceptionMessage.value(), StandardCharsets.UTF_8))
                .contains("Event and eventType must not be null or blank");

        // Verify retries were executed according to policy:
        // maxAttempts = 2 retries (total 3 delivery attempts: 1 initial + 2 retries)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(TestRetryConfig.deliveryAttemptsByKey.get(key)).containsExactly(1, 2, 3);
        });
    }
}
