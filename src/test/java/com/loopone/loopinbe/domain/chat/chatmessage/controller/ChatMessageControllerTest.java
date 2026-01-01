package com.loopone.loopinbe.domain.chat.chatmessage.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.chat.chatMessage.controller.ChatMessageController;
import com.loopone.loopinbe.domain.chat.chatMessage.dto.res.ChatMessageResponse;
import com.loopone.loopinbe.domain.chat.chatMessage.entity.ChatMessage;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.config.SecurityConfig;
import com.loopone.loopinbe.global.config.WebConfig;
import com.loopone.loopinbe.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ChatMessageControllerTest {
    CurrentUserDto mockUser;
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ChatMessageService chatMessageService;
    @MockitoBean
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {

        mockUser = new CurrentUserDto(
                1L, "aaa@bbb.com", null, "jun",
                "010-2222-3333", Member.Gender.MALE,
                LocalDate.of(2000, 1, 1), null,
                Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.GOOGLE, "pid"
        );

        given(currentUserArgumentResolver.supportsParameter(any()))
                .willReturn(true);

        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(mockUser);
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-message/{chatRoomId} → 채팅방 과거 메시지 조회 성공")
    void findByChatRoomId_success() throws Exception {

        // --- GIVEN ---
        ChatMessageResponse dto = fakeChat();

        Pageable pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(dto), pageable, 1);

        PageResponse<ChatMessageResponse> pageResponse = PageResponse.of(page);

        given(chatMessageService.findByChatRoomId(eq(10L), any(Pageable.class), any()))
                .willReturn(pageResponse);

        // --- WHEN & THEN ---
        mvc.perform(get("/rest-api/v1/chat-message/{chatRoomId}", 10L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].content").value("hello"));
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-message/{chatRoomId}/search → 메시지 검색 성공")
    void searchChatMessage_success() throws Exception {

        // --- GIVEN ---
        ChatMessageResponse dto = fakeChat();

        Pageable pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(dto), pageable, 1);

        PageResponse<ChatMessageResponse> pageResponse = PageResponse.of(page);

        given(chatMessageService.searchByKeyword(eq(10L), eq("hello"), any(Pageable.class), any()))
                .willReturn(pageResponse);

        // --- WHEN & THEN ---
        mvc.perform(get("/rest-api/v1/chat-message/{chatRoomId}/search", 10L)
                        .param("keyword", "hello")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].content").value("hello"));
    }

    private static ChatMessageResponse fakeChat() {
        return ChatMessageResponse.builder()
                .id(1L)
                .content("hello")
                .authorType(ChatMessage.AuthorType.USER)  // 보통 필요함
                .createdAt(LocalDateTime.now())
                .build();
    }
}
