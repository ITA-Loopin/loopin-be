package com.loopone.loopinbe.domain.chat.chatMessage.converter;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.ChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ChatMessageConverter {
    @Autowired
    protected MemberRepository memberRepository;
    private static final String BOT_NICKNAME = "loopin";
    private static final String BOT_PROFILE = null;

    // ---------- ChatMessage -> ChatMessageResponse ----------
    @Mapping(target = "id", source = "id")
    @Mapping(target = "memberId", source = "memberId")
    @Mapping(target = "nickname", expression = "java(resolveNickname(chatMessage, memberMap))")
    @Mapping(target = "profileImageUrl", expression = "java(resolveProfile(chatMessage, memberMap))")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "imageUrls", source = "imageUrls")
    @Mapping(target = "recommendations", source = "recommendations")
    @Mapping(target = "authorType", source = "authorType")
    @Mapping(target = "createdAt", source = "createdAt")
    public abstract ChatMessageResponse toChatMessageResponse(
            ChatMessage chatMessage,
            @Context Map<Long, Member> memberMap
    );

    // ---------- Member bulk load ----------
    public Map<Long, Member> loadMembers(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return Collections.emptyMap();
        Set<Long> memberIds = messages.stream()
                .map(ChatMessage::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (memberIds.isEmpty()) return Collections.emptyMap();
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    // ---------- helpers ----------
    protected String resolveNickname(ChatMessage chatMessage, @Context Map<Long, Member> memberMap) {
        if (isBot(chatMessage)) return BOT_NICKNAME;
        Long memberId = chatMessage.getMemberId();
        Member m = (memberId == null) ? null : memberMap.get(memberId);

        // 탈퇴/유실 등으로 못 찾는 경우도 대비
        return (m != null && m.getNickname() != null) ? m.getNickname() : "unknown";
    }

    protected String resolveProfile(ChatMessage chatMessage, @Context Map<Long, Member> memberMap) {
        if (isBot(chatMessage)) return BOT_PROFILE;
        Long memberId = chatMessage.getMemberId();
        Member m = (memberId == null) ? null : memberMap.get(memberId);
        return (m != null) ? m.getProfileImageUrl() : null;
    }

    protected boolean isBot(ChatMessage chatMessage) {
        return chatMessage.getAuthorType() == ChatMessage.AuthorType.BOT || chatMessage.getMemberId() == null;
    }
}
