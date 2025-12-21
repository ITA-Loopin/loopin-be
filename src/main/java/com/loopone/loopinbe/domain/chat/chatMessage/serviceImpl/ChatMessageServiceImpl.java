package com.loopone.loopinbe.domain.chat.chatMessage.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.converter.ChatMessageConverter;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageDto;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessagePayload;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageRequest;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.ChatMessageSavedResult;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessagePage;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.MessageContent;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.MessageContentRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.mapper.LoopMapper;
import com.loopone.loopinbe.domain.sse.service.SseEmitterService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.ai.AiEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType.CREATE_LOOP;
import static com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType.UPDATE_LOOP;
import static com.loopone.loopinbe.global.constants.KafkaKey.OPEN_AI_CREATE_TOPIC;
import static com.loopone.loopinbe.global.constants.KafkaKey.OPEN_AI_UPDATE_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final MessageContentRepository messageContentRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageConverter chatMessageConverter;
    private final AiEventPublisher aiEventPublisher;
    private final LoopMapper loopMapper;
    private final SseEmitterService sseEmitterService;

    // 채팅방 과거 메시지 조회 [참여자 권한]
    @Override
    @Transactional
    public PageResponse<ChatMessageDto> findByChatRoomId(Long chatRoomId, Pageable pageable, CurrentUserDto currentUser) {
        try {
            checkPageSize(pageable.getPageSize());
            ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByMemberIdAndChatRoomId(currentUser.id(), chatRoomId);
            if (chatRoomMember == null) {
                log.warn("chatRoomMember not found - chatRoomId: {}, loginUserId: {}", chatRoomId, currentUser.id());
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            Page<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomId(chatRoomId, sortedPageable);
            List<String> stringMessageIds = chatMessages.stream()
                    .map(chatMessage -> String.valueOf(chatMessage.getMessageKey()))
                    .collect(Collectors.toList());
            List<MessageContent> contents = Optional.ofNullable(messageContentRepository.findByIdIn(stringMessageIds))
                    .orElse(Collections.emptyList());
            Map<String, String> messageContentMap = new HashMap<>();
            Map<String, List<LoopCreateRequest>> recommendationsLoopMap = new HashMap<>();
            for (MessageContent message : contents) {
                try {
                    messageContentMap.put(message.getId(), message.getContent());
                    recommendationsLoopMap.put(message.getId(), message.getRecommendations());
                } catch (NumberFormatException e) {
                    log.warn("Invalid messageContent ID format: {}", message.getId());
                }
            }
            return PageResponse.of(chatMessages.map(chatMessage -> {
                String content = messageContentMap.getOrDefault(chatMessage.getMessageKey(), "");
                List<LoopCreateRequest> recommendationsLoop = recommendationsLoopMap.get(chatMessage.getMessageKey());
                return chatMessageConverter.toChatMessageDto(chatMessage, content, recommendationsLoop);
            }));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in findByChatRoomId - chatRoomId: {}, loginUserId: {}, error: {}", chatRoomId, currentUser.id(), e.getMessage(), e);
            throw new ServiceException(ReturnCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 채팅방 메시지 검색(내용) [참여자 권한]
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageDto> searchByKeyword(Long chatRoomId, String keyword, Pageable pageable, CurrentUserDto currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ServiceException(ReturnCode.CHATROOM_NOT_FOUND));
        checkPageSize(pageable.getPageSize());

        // 채팅방 참여멤버만 메시지 조회 가능
        boolean memberExists = chatRoom.getChatRoomMembers().stream()
                .anyMatch(joinedMember -> joinedMember.getMember().getId().equals(currentUser.id()));
        if (!memberExists) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }

        // 1. JPA로 메시지 메타 조회 (fetch join 적용, N+1 방지)
        Page<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomIdWithMembers(chatRoomId, pageable);
        List<String> stringMessageIds = chatMessages.stream()
                .map(cm -> String.valueOf(cm.getId()))
                .toList();

        // 2. MongoDB에서 content + keyword 조건 검색
        List<MessageContent> contents = messageContentRepository.findByIdInAndContentContaining(stringMessageIds, keyword);
        Map<Long, String> messageContentMap = contents.stream()
                .collect(Collectors.toMap(mc -> Long.parseLong(mc.getId()), MessageContent::getContent));

        // 3. 메시지 본문이 존재하는 것만 필터링
        List<ChatMessage> filteredMessages = chatMessages.stream()
                .filter(cm -> messageContentMap.containsKey(cm.getId()))
                .toList();

        // 4. DTO 변환
        List<ChatMessageDto> responses = filteredMessages.stream()
                .map(cm -> chatMessageConverter.toChatMessageDto(cm, messageContentMap.get(cm.getId()), null))
                .toList();
        return PageResponse.of(new PageImpl<>(responses, pageable, responses.size()));
    }

    // Kafka 인바운드 메시지 처리(권한검증 + 멱등 저장 + Mongo 업서트)
    @Override
    @Transactional
    public ChatMessageSavedResult processInbound(ChatMessagePayload in) {
        log.info("Mongo업서트 요청 처리 시작: messageKey={}", in.messageKey());
        // 1) 권한 검증 (비재시도 예외로 던지는 게 운영에 유리)
        // BOT 메시지는 멤버 검증을 생략
        if (in.authorType() != ChatMessage.AuthorType.BOT) {
            if (!chatRoomRepository.existsMember(in.chatRoomId(), in.memberId())) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
        }

        ChatRoom roomRef = chatRoomRepository.getReferenceById(in.chatRoomId());
        // 2) RDB 멱등 저장 (message_key UNIQUE)
        ChatMessage msg = chatMessageRepository.findByMessageKey(in.messageKey())
                .orElseGet(() -> {
                    try {
                        ChatRoom room = chatRoomRepository.getReferenceById(in.chatRoomId());

                        Member member = (in.authorType() == ChatMessage.AuthorType.BOT)
                                ? null
                                : memberRepository.getReferenceById(in.memberId());

                        return chatMessageRepository.save(
                                ChatMessage.builder()
                                        .messageKey(in.messageKey())
                                        .chatRoom(room)
                                        .member(member)
                                        .authorType(in.authorType())
                                        .build()
                        );
                    } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                        // 동시/재시도 경합 시 재조회
                        return chatMessageRepository.findByMessageKey(in.messageKey())
                                .orElseThrow(() -> dup);
                    }
                });
        // 3) Mongo 업서트 (id = messageKey)
        messageContentRepository.upsert(in.messageKey(), in.content(), in.recommendations());
        log.info("Mongo업서트 완료: messageKey={}", in.messageKey());
        // 4) 봇 방 여부는 ChatRoom에서!
        boolean isBotRoom = (msg.getChatRoom() != null)
                ? msg.getChatRoom().isBotRoom()
                : roomRef.isBotRoom(); // 안전 차원
        return new ChatMessageSavedResult(
                in.chatRoomId(),
                in.memberId(),
                msg.getId(),
                in.content(),
                in.recommendations(),
                in.authorType(),
                msg.getCreatedAt(),
                isBotRoom
        );
    }

    // 채팅방의 모든 메시지 삭제
    @Override
    @Transactional
    public void deleteAllChatMessages(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHATROOM_NOT_FOUND));
        List<ChatMessage> messages = chatMessageRepository.findByChatRoom(chatRoom);

        messages.forEach(message -> {
            messageContentRepository.deleteById(String.valueOf(message.getId()));
        });
        chatMessageRepository.deleteAll(messages);
    }

    @Override
    @Transactional
    public void sendChatMessage(Long chatRoomId, ChatMessageRequest request, CurrentUserDto currentUser) {
        if (request.type() != CREATE_LOOP && request.type() != UPDATE_LOOP) {
            throw new ServiceException(ReturnCode.CHATMESSAGE_INVALID_TYPE);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHATROOM_NOT_FOUND));

        String content = (request.message() != null) ? request.message() : null;
        if (content == null || content.isBlank()) {
            throw new ServiceException(ReturnCode.CHATMESSAGE_EMPTY_CONTENT);
        }

        ChatMessagePayload payload = toChatMessagePayload(chatRoomId, currentUser.id(), content);

        ChatMessageSavedResult saved = processInbound(payload);

        sseEmitterService.sendToClient(chatRoomId, MessageType.MESSAGE, payload);

        Loop loop = chatRoom.getLoop();
        LoopDetailResponse loopDetailResponse = (loop != null) ? loopMapper.toDetailResponse(loop) : null;

        if (request.type() == CREATE_LOOP) {
            publishAI(saved, loopDetailResponse, OPEN_AI_CREATE_TOPIC);
        } else {
            publishAI(saved, loopDetailResponse, OPEN_AI_UPDATE_TOPIC);
        }
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = ChatMessagePage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    private ChatMessagePayload toChatMessagePayload(Long chatRoomId, Long userId, String content) {
        return new ChatMessagePayload(
                java.util.UUID.randomUUID().toString(),
                chatRoomId,
                userId,
                content,
                null,
                ChatMessage.AuthorType.USER,
                java.time.LocalDateTime.now());
    }

    private void publishAI(ChatMessageSavedResult saved, LoopDetailResponse loopDetailResponse, String topic) {
        AiPayload req = new AiPayload(
                java.util.UUID.randomUUID().toString(),
                saved.chatRoomId(),
                saved.messageId(),
                saved.memberId(),
                saved.content(),
                loopDetailResponse,
                java.time.Instant.now());
        aiEventPublisher.publishAiRequest(req, topic);
    }
}
