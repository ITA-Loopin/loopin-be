package com.loopone.loopinbe.domain.chat.chatMessage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.req.AttachmentRequest;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.req.ChatMessageRequest;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.ChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.type.MessageType;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.config.SecurityConfig;
import com.loopone.loopinbe.global.config.WebConfig;
import com.loopone.loopinbe.global.security.JwtAuthenticationFilter;
import com.loopone.loopinbe.global.security.TokenResolver;
import com.loopone.loopinbe.global.web.cookie.WebAuthCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ChatMessageController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        JwtAuthenticationFilter.class,
                        SecurityConfig.class,
                        WebConfig.class
                })
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ChatMessageControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ChatMessageService chatMessageService;
    @MockitoBean CurrentUserArgumentResolver currentUserArgumentResolver;
    @MockitoBean TokenResolver tokenResolver;
    @MockitoBean WebAuthCookieFactory webAuthCookieFactory;

    @BeforeEach
    void setUp() throws Exception {
        given(currentUserArgumentResolver.supportsParameter(any()))
                .willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new CurrentUserDto(
                        1L, "user@loop.in", null, "user", "010-0000-0000",
                        Member.Gender.MALE, LocalDate.of(2000,1,1),
                        null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                        Member.OAuthProvider.GOOGLE, "provider-id"
                ));
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-message/{chatRoomId} → 200 OK")
    void findByChatRoomId_success() throws Exception {
        Long chatRoomId = 100L;
        ChatMessageResponse msg = new ChatMessageResponse();
        msg.setId("msg-1");
        msg.setContent("hello");

        PageResponse<ChatMessageResponse> pageResp = PageResponse.of(
                new PageImpl<>(List.of(msg), PageRequest.of(0, 20), 1)
        );

        given(chatMessageService.findByChatRoomId(eq(chatRoomId), any(Pageable.class), any(CurrentUserDto.class)))
                .willReturn(pageResp);

        mvc.perform(get("/rest-api/v1/chat-message/{chatRoomId}", chatRoomId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("msg-1"))
                .andExpect(jsonPath("$.data[0].content").value("hello"));
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-message/{chatRoomId}/search → 200 OK")
    void searchChatMessage_success() throws Exception {
        Long chatRoomId = 100L;
        String keyword = "test";
        ChatMessageResponse msg = new ChatMessageResponse();
        msg.setId("msg-2");
        msg.setContent("test content");

        PageResponse<ChatMessageResponse> pageResp = PageResponse.of(
                new PageImpl<>(List.of(msg), PageRequest.of(0, 20), 1)
        );

        given(chatMessageService.searchByKeyword(eq(chatRoomId), eq(keyword), any(Pageable.class), any(CurrentUserDto.class)))
                .willReturn(pageResp);

        mvc.perform(get("/rest-api/v1/chat-message/{chatRoomId}/search", chatRoomId)
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("msg-2"))
                .andExpect(jsonPath("$.data[0].content").value("test content"));
    }

    @Test
    @DisplayName("POST /rest-api/v1/chat-message/{chatRoomId}/chat → 200 OK")
    void sendChatMessage_success() throws Exception {
        Long chatRoomId = 100L;
        ChatMessageRequest req = new ChatMessageRequest("hello", UUID.randomUUID(), MessageType.CREATE_LOOP);

        mvc.perform(post("/rest-api/v1/chat-message/{chatRoomId}/chat", chatRoomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(chatMessageService).sendChatMessage(eq(chatRoomId), any(ChatMessageRequest.class), any(CurrentUserDto.class));
    }

    @Test
    @DisplayName("POST /rest-api/v1/chat-message/attachments/{chatRoomId} → 200 OK")
    void sendAttachment_success() throws Exception {
        Long chatRoomId = 100L;
        UUID clientMessageId = UUID.randomUUID();
        AttachmentRequest attachmentRequest = new AttachmentRequest(clientMessageId);

        MockMultipartFile jsonPart = new MockMultipartFile(
                "attachmentRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(attachmentRequest)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "images", "img.png", MediaType.IMAGE_PNG_VALUE, "img".getBytes()
        );

        mvc.perform(multipart("/rest-api/v1/chat-message/attachments/{chatRoomId}", chatRoomId)
                        .file(jsonPart)
                        .file(imagePart)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<List<MultipartFile>> filesCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatMessageService).sendAttachment(
                eq(chatRoomId),
                eq(clientMessageId),
                filesCaptor.capture(),
                eq(null),
                any(CurrentUserDto.class)
        );
        assertThat(filesCaptor.getValue()).hasSize(1);
    }
}