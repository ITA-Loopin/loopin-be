package com.loopone.loopinbe.support;

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

    // PostgreSQL → spring.datasource.* 자동 연결
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    // MongoDB → spring.data.mongodb.uri 자동 연결
    @Bean
    @ServiceConnection
    MongoDBContainer mongodb() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }

    // Kafka(Redpanda 대체 테스트) → spring.kafka.bootstrap-servers 자동 연결
    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    }

    // Redis → spring.redis.host/port 자동 연결 (Boot 3.2+에서 지원)
    @Bean
    @ServiceConnection
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
