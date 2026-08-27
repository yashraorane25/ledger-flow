package com.ledgerflow.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentsEventsTopic() {
        return TopicBuilder.name("payments.events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
