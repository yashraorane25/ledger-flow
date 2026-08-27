package com.ledgerflow.ledger_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Also declared by payment-service. NewTopic beans are create-if-missing,
    // so whichever service starts first creates it with the right partition
    // count — without this, if ledger-service started first, Kafka's
    // auto-create-topics would silently create it with the broker default
    // partition count instead of the 3 CLAUDE.md specifies.
    @Bean
    public NewTopic paymentsEventsTopic() {
        return TopicBuilder.name("payments.events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentsDltTopic() {
        return TopicBuilder.name("payments.dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
