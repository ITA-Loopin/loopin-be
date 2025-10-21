package com.loopone.loopinbe.domain.chat.chatMessage.repository;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageContentRepository extends MongoRepository<MessageContent, String>, MessageContentRepositoryCustom {
    // MongoDB에서 메시지 내용 불러오기
    List<MessageContent> findByIdIn(List<String> messageIds);

    List<MessageContent> findByIdInAndContentContaining(List<String> ids, String keyword);
}
