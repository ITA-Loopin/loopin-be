package com.loopone.loopinbe.domain.chat.chatMessage.repositoryImpl;

import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatAttachment;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageMongoRepositoryCustom;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class ChatMessageMongoRepositoryImpl implements ChatMessageMongoRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    // 채팅 내용 저장
    @Override
    public ChatMessage upsertInbound(
            String id,
            String clientMessageId,
            Long chatRoomId,
            Long memberId,
            String content,
            List<ChatAttachment> attachments,
            List<LoopCreateRequest> recommendations,
            Long loopRuleId,
            String deleteMessageId,
            ChatMessage.AuthorType authorType,
            Instant createdAt,
            Instant modifiedAt
    ) {
        Query q = new Query(Criteria.where("_id").is(id));
        Update u = new Update()
                // 멱등: 최초 삽입시에만 고정되는 필드
                .setOnInsert("_id", id)
                .setOnInsert("clientMessageId", clientMessageId)
                .setOnInsert("chatRoomId", chatRoomId)
                .setOnInsert("memberId", memberId)
                .setOnInsert("content", content)
                .setOnInsert("attachments", attachments)
                .setOnInsert("recommendations", recommendations)
                .setOnInsert("loopRuleId", loopRuleId)
                .setOnInsert("deleteMessageId", deleteMessageId)
                .setOnInsert("authorType", authorType)
                .setOnInsert("createdAt", createdAt)
                .setOnInsert("modifiedAt", modifiedAt);
        FindAndModifyOptions opt = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        return mongoTemplate.findAndModify(q, u, opt, ChatMessage.class);
    }

    // 채팅방 내 내용 검색 (Mongo 텍스트 인덱스 사용)
    @Override
    public Page<ChatMessage> searchByKeyword(Long chatRoomId, String keyword, Pageable pageable) {
        if (chatRoomId == null) throw new IllegalArgumentException("chatRoomId is null");
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }

        // keyword의 "문자 하나라도 포함" -> (문자1|문자2|...) OR 정규식
        List<String> chars = new ArrayList<>();
        keyword.codePoints().forEach(cp -> chars.add(new String(Character.toChars(cp))));

        // 각 문자를 regex 안전하게 escape - 예: "." 같은 문자가 들어와도 리터럴로 취급
        String alternation = chars.stream()
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElseThrow();

        Pattern pattern = Pattern.compile(alternation); // 대소문자 무시 필요하면 Pattern.CASE_INSENSITIVE 추가

        Criteria criteria = Criteria.where("chatRoomId").is(chatRoomId)
                .and("content").regex(pattern);

        Query query = new Query(criteria).with(pageable);

        List<ChatMessage> content = mongoTemplate.find(query, ChatMessage.class);

        // count는 pageable 제외하고 계산해야 함
        Query countQuery = Query.of(query).limit(-1).skip(-1);
        long total = mongoTemplate.count(countQuery, ChatMessage.class);

        return new PageImpl<>(content, pageable, total);
    }
}
