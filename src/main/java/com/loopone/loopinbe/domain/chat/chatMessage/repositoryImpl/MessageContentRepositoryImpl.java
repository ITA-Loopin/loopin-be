package com.loopone.loopinbe.domain.chat.chatMessage.repositoryImpl;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MessageContentRepositoryImpl implements MessageContentRepositoryCustom {

    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public void upsert(String id, String content) {
        var query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)
        );
        var update = new org.springframework.data.mongodb.core.query.Update()
                .setOnInsert("_id", id)
                .set("content", content);
        mongoTemplate.upsert(query, update, MessageContent.class);
    }
}
