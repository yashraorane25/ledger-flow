package com.ledgerflow.ledger_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

// A few quick retries handle transient blips (e.g. a momentary DB hiccup)
// without giving up immediately. If the message still fails after those,
// DeadLetterPublishingRecoverer republishes it to payments.dlt instead of
// leaving Kafka to redeliver the same poison message forever, which would
// otherwise block every message behind it on that partition.
//
// Deliberately NOT named "kafkaListenerContainerFactory" (Spring Boot's
// default factory bean name) — that would make every @KafkaListener in this
// service use it, including DeadLetterHandler's own listener on payments.dlt.
// If the DLT handler itself ever failed, this recoverer would republish the
// message right back onto payments.dlt, looping forever. Only PaymentEventsConsumer
// opts into this factory explicitly.
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqCapableListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition("payments.dlt", -1));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
