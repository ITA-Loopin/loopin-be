package com.loopone.loopinbe.domain.chat.chatMessage.repository;

import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessage, String>,  ChatMessageMongoRepositoryCustom{
    Page<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);

    Optional<ChatMessage> findByClientMessageId(String clientMessageId);

    long deleteByChatRoomId(Long chatRoomId);
}
