package com.loopone.loopinbe.domain.chat.chatMessage.repositoryImpl;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepositoryCustom;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class MessageContentRepositoryImpl implements MessageContentRepositoryCustom {

    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public void upsert(String id, String content, List<LoopCreateRequest> recommendations) {
        var query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)
        );
        var update = new org.springframework.data.mongodb.core.query.Update()
                .setOnInsert("_id", id)
                .set("content", content);

        if(recommendations != null) {
            update.set("recommendations", recommendations);
        }
        mongoTemplate.upsert(query, update, MessageContent.class);
    }
}
