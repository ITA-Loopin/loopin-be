package com.loopone.loopinbe.domain.chat.chatMessage.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.req.ChatMessageRequest;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.ChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.domain.chat.chatMessage.repository.ChatMessageMongoRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.serviceImpl.ChatMessageServiceImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoom;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomStateService;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.mapper.LoopMapper;
import com.loopone.loopinbe.domain.sse.service.SseEmitterService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.kafka.event.ai.AiEventPublisher;
import com.loopone.loopinbe.global.kafka.event.chatMessage.ChatMessageEventPublisher;
import com.loopone.loopinbe.global.s3.S3Service;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {"testcontainers.mongo.enabled=true", "testcontainers.kafka.enabled=true", "spring.data.mongodb.auto-index-creation=true"})
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Transactional
class ChatMessageServiceTest {

    @Autowired ChatMessageServiceImpl chatMessageService;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageMongoRepository chatMessageMongoRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository loopRepository;
    @Autowired com.loopone.loopinbe.domain.loop.loop.repository.LoopRuleRepository loopRuleRepository;

    @MockitoBean AiEventPublisher aiEventPublisher;
    @MockitoBean SseEmitterService sseEmitterService;
    @MockitoBean S3Service s3Service;
    @MockitoBean ChatMessageEventPublisher chatMessageEventPublisher;
    @MockitoBean LoopMapper loopMapper;
    @MockitoBean ChatRoomStateService chatRoomStateService;
    @MockitoBean(name = "geminiChatModel") org.springframework.ai.chat.model.ChatModel geminiChatModel;
    @MockitoBean(name = "openAiChatModel") org.springframework.ai.chat.model.ChatModel openAiChatModel;
    @MockitoBean com.loopone.loopinbe.global.initData.notProd.service.NotProdService notProdService;
    @MockitoBean com.loopone.loopinbe.global.kafka.event.ai.AiEventConsumer aiEventConsumer;

    private Member member;
    private ChatRoom chatRoom;
    private CurrentUserDto currentUser;

    @BeforeEach
    void setUp() {
        chatMessageMongoRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        loopRepository.deleteAll();
        loopRuleRepository.deleteAll();
        memberRepository.deleteAll();

        member = Member.builder()
                .email("user@loop.in")
                .nickname("user")
                .build();
        memberRepository.save(member);

        currentUser = new CurrentUserDto(
                member.getId(), member.getEmail(), null, member.getNickname(), null,
                null, null, null, member.getState(), member.getRole(), member.getOAuthProvider(), member.getProviderId()
        );

        chatRoom = ChatRoom.builder()
                .title("Test Room")
                .member(member) // owner
                .isBotRoom(true)
                .build();
        
        chatRoomRepository.save(chatRoom);
        
        ChatRoomMember crm = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatRoomMemberRepository.save(crm);
    }

    @AfterEach
    void tearDown() {
        chatMessageMongoRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        loopRepository.deleteAll();
        loopRuleRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("채팅방 메시지 전송 및 저장 (CREATE_LOOP)")
    void sendChatMessage_createLoop() {
        // given
        given(loopMapper.toDetailResponse(any(Loop.class))).willReturn(null);
        UUID clientMessageId = UUID.randomUUID();
        ChatMessageRequest request = new ChatMessageRequest("content", clientMessageId, MessageType.CREATE_LOOP);

        // when
        chatMessageService.sendChatMessage(chatRoom.getId(), request, currentUser);

        // then
        List<ChatMessage> messages = chatMessageMongoRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("content");
        assertThat(messages.get(0).getClientMessageId()).isEqualTo(clientMessageId);
    }

    @Test
    @DisplayName("채팅방 메시지 조회")
    void findByChatRoomId() {
        // given
        ChatMessage msg1 = ChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .memberId(member.getId())
                .content("msg1")
                .build();
        msg1.setCreatedAt(java.time.Instant.now().minusSeconds(10));
        ChatMessage msg2 = ChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .memberId(member.getId())
                .content("msg2")
                .build();
        msg2.setCreatedAt(java.time.Instant.now());
        chatMessageMongoRepository.saveAll(List.of(msg1, msg2));

        // when
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<ChatMessageResponse> result = chatMessageService.findByChatRoomId(chatRoom.getId(), pageable, currentUser);

        // then
        assertThat(result.getPageMeta().getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        // 내림차순 정렬 확인 (최신순)
        assertThat(result.getContent().get(0).getContent()).isEqualTo("msg2");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("msg1");
    }

    @Test
    @DisplayName("메시지 검색")
    void searchByKeyword() {
        // given
        chatMessageMongoRepository.save(ChatMessage.builder().chatRoomId(chatRoom.getId()).memberId(member.getId()).content("hello world").build());
        chatMessageMongoRepository.save(ChatMessage.builder().chatRoomId(chatRoom.getId()).memberId(member.getId()).content("bye world").build());

        // when
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<ChatMessageResponse> result = chatMessageService.searchByKeyword(chatRoom.getId(), "hello", pageable, currentUser);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("hello world");
    }

    @Test
    @DisplayName("추천 메시지 삭제 테스트")
    void deleteRecommendationMessage() {
        // given
        ChatMessage botMsg = ChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .authorType(ChatMessage.AuthorType.BOT)
                .content("recommendation")
                .recommendations(Collections.singletonList(null)) // mock list not empty
                .build();
        chatMessageMongoRepository.save(botMsg);

        // when
        String deletedId = chatMessageService.deleteRecommendationMessage(chatRoom.getId());

        // then
        assertThat(chatMessageMongoRepository.findById(botMsg.getId())).isEmpty();
    }
}
