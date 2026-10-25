package com.aniket.userpulse;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private final long retryBackoffMs;
    private final long retryMaxAttempts;

    public KafkaConfig(
            @Value("${userpulse.kafka.retry.backoff-ms:1000}") long retryBackoffMs,
            @Value("${userpulse.kafka.retry.max-attempts:2}") long retryMaxAttempts) {
        this.retryBackoffMs = retryBackoffMs;
        this.retryMaxAttempts = retryMaxAttempts;
    }

    @Bean
    public NewTopic userActivityTopic() {
        return TopicBuilder.name("user-activity")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userActivityDltTopic() {
        return TopicBuilder.name("user-activity.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            ProducerFactory<Object, Object> producerFactory) {

        Map<String, Object> producerConfigs = new HashMap<>(producerFactory.getConfigurationProperties());

        ProducerFactory<Object, byte[]> bytesProducerFactory = new DefaultKafkaProducerFactory<>(
                producerConfigs,
                new DelegatingByTypeSerializer(Map.of(
                        byte[].class, new ByteArraySerializer(),
                        Object.class, new StringSerializer()
                ), true),
                new ByteArraySerializer()
        );
        KafkaTemplate<Object, byte[]> bytesKafkaTemplate = new KafkaTemplate<>(bytesProducerFactory);

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesKafkaTemplate);
        templates.put(Object.class, kafkaTemplate);

        return new DeadLetterPublishingRecoverer(
                templates,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer,
            ObjectProvider<RetryListener> retryListeners) {

        FixedBackOff backOff = new FixedBackOff(retryBackoffMs, retryMaxAttempts);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);
        retryListeners.forEach(errorHandler::setRetryListeners);
        return errorHandler;
    }
}
