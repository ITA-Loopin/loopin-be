package com.loopone.loopinbe.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

import static com.loopone.loopinbe.global.constants.KafkaKey.OPEN_AI_TOPIC;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    public NewTopic gptTopic() {
        return TopicBuilder.name(OPEN_AI_TOPIC).partitions(3).replicas(1).build();
    }
}
