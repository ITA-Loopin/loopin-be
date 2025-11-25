package com.loopone.loopinbe.domain.account.chat.chatmessage.repository;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepositoryCustom;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(TestContainersConfig.class)
@TestPropertySource(properties = {
        "testcontainers.mongo.enabled=true",
        "testcontainers.redis.enabled=false",
        "testcontainers.kafka.enabled=false"
})
class MessageContentRepositoryCustomTest {

    @Autowired
    MessageContentRepository messageContentRepository;

    @Autowired
    @Qualifier("messageContentRepositoryImpl")
    MessageContentRepositoryCustom messageRepositoryCustom;

    @Test
    @DisplayName("upsert() - 신규 문서 생성 확인")
    void upsert_insert() {
        // GIVEN
        String id = "msg1";
        String content = "hello-mongo";
        List<LoopCreateRequest> recommendations = List.of(fakeLoop());

        // WHEN
        messageRepositoryCustom.upsert(id, content, recommendations);

        // THEN
        MessageContent saved = messageContentRepository.findById(id).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getContent()).isEqualTo("hello-mongo");
        assertThat(saved.getRecommendations()).hasSize(1);
    }

    @Test
    @DisplayName("upsert() - 기존 문서 업데이트 수행 (content 변경, recommendations 변경)")
    void upsert_update() {
        // 기존 데이터 저장
        String id = "msg2";
        messageContentRepository.save(new MessageContent(id, "old", List.of()));

        // WHEN
        messageRepositoryCustom.upsert(
                id,
                "new-content",
                List.of(fakeLoop())
        );

        // THEN
        MessageContent updated = messageContentRepository.findById(id).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("new-content");
        assertThat(updated.getRecommendations()).hasSize(1);
    }

    @Test
    @DisplayName("upsert() - recommendations null일 경우 기존 필드 유지")
    void upsert_update_recommendationsNull() {
        // 기존 데이터 저장
        String id = "msg3";
        messageContentRepository.save(new MessageContent(id, "before", List.of(fakeLoop())));

        // WHEN — recommendations = null
        messageRepositoryCustom.upsert(id, "changed-content", null);

        // THEN
        MessageContent updated = messageContentRepository.findById(id).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("changed-content");
        assertThat(updated.getRecommendations())
                .hasSize(1)
                .extracting(LoopCreateRequest::title)
                .containsExactly("title");
    }

    public static LoopCreateRequest fakeLoop() {
        return new LoopCreateRequest(
                "title",
                "content",
                RepeatType.NONE,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                List.of()
        );
    }
}