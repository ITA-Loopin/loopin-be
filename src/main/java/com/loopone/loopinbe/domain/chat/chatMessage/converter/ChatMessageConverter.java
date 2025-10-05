package com.loopone.loopinbe.domain.chat.chatMessage.converter;

import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMessageConverter {
    // ---------- ChatMessage -> ChatMessageDto ----------
    @Mapping(target = "id",             source = "chatMessage.id")
    @Mapping(target = "memberId",       source = "chatMessage.member.id")
    @Mapping(target = "nickname",       source = "chatMessage.member.nickname")
    @Mapping(target = "profileImageUrl",source = "chatMessage.member.profileImageUrl")
    @Mapping(target = "content",        expression = "java(content)")
    @Mapping(target = "createdAt",      source = "chatMessage.createdAt")
    ChatMessageDto toChatMessageDto(ChatMessage chatMessage, String content);
}
