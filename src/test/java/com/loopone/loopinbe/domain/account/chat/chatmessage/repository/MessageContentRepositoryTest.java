package com.loopone.loopinbe.domain.account.chat.chatmessage.repository;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepository;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(TestContainersConfig.class)
@TestPropertySource(properties = {
        "testcontainers.mongo.enabled=true",
        "testcontainers.redis.enabled=false",
        "testcontainers.kafka.enabled=false"
})
@ActiveProfiles("test")
class MessageContentRepositoryTest {
    @Autowired
    private MessageContentRepository messageContentRepository;

    @BeforeEach
    void clean() {
        messageContentRepository.deleteAll();
    }

    @Test
    @DisplayName("findByIdIn - 지정된 messageId 목록으로 조회 성공")
    void findByIdIn_success() {

        // given
        MessageContent m1 = new MessageContent("m1", "hello world", List.of());
        MessageContent m2 = new MessageContent("m2", "bye world", List.of());

        messageContentRepository.save(m1);
        messageContentRepository.save(m2);

        // when
        List<MessageContent> result =
                messageContentRepository.findByIdIn(List.of("m1", "m2"));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(MessageContent::getId)
                .containsExactlyInAnyOrder("m1", "m2");
    }

    @Test
    @DisplayName("findByIdInAndContentContaining - content keyword 검색 성공")
    void findByIdInAndContentContaining_success() {

        // given
        MessageContent m1 = new MessageContent("k1", "user said hello", List.of());
        MessageContent m2 = new MessageContent("k2", "system message", List.of());
        MessageContent m3 = new MessageContent("k3", "hi hello again", List.of());

        messageContentRepository.save(m1);
        messageContentRepository.save(m2);
        messageContentRepository.save(m3);

        // when: "hello" 포함된 메시지 + id 조건
        List<MessageContent> result =
                messageContentRepository.findByIdInAndContentContaining(
                        List.of("k1", "k3"),
                        "hello"
                );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(MessageContent::getId)
                .containsExactlyInAnyOrder("k1", "k3");
    }
}