package com.loopone.loopinbe.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {
    @Bean @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(value = "testcontainers.mongo.enabled", havingValue = "true")
    MongoDBContainer mongodb() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(value = "testcontainers.kafka.enabled", havingValue = "true")
    KafkaContainer kafka() {
        DockerImageName img = DockerImageName
                .parse("apache/kafka:3.7.0"); // ← 안전한 기본
        return new KafkaContainer(img);
    }

    @Bean
    @ServiceConnection(name = "redis")
    @ConditionalOnProperty(value = "testcontainers.redis.enabled", havingValue = "true")
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
