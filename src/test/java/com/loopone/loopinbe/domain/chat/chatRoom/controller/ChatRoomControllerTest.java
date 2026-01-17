package com.loopone.loopinbe.domain.chat.chatRoom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomListResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.enums.ChatRoomType;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.global.config.SecurityConfig;
import com.loopone.loopinbe.global.config.WebConfig;
import com.loopone.loopinbe.global.security.JwtAuthenticationFilter;
import com.loopone.loopinbe.global.security.TokenResolver;
import com.loopone.loopinbe.global.web.cookie.WebAuthCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChatRoomController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        JwtAuthenticationFilter.class,
                        SecurityConfig.class
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
class ChatRoomControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean CurrentUserArgumentResolver currentUserArgumentResolver;
    @MockitoBean TokenResolver tokenResolver;
    @MockitoBean WebAuthCookieFactory webAuthCookieFactory;

    @BeforeEach
    void setUp() throws Exception {
        given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new CurrentUserDto(
                        1L, "user@loop.in", null, "user", "010-0000-0000",
                        Member.Gender.MALE, LocalDate.of(2000,1,1),
                        null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                        Member.OAuthProvider.GOOGLE, "provider-id"
                ));
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-room?chatRoomType=ALL → 200 OK")
    void getChatRooms_success() throws Exception {
        ChatRoomListResponse resp = new ChatRoomListResponse(Collections.emptyList());
        given(chatRoomService.getChatRooms(eq(1L), eq(ChatRoomType.ALL))).willReturn(resp);

        mvc.perform(get("/rest-api/v1/chat-room")
                        .param("chatRoomType", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRooms").isArray());
    }

    @Test
    @DisplayName("POST /rest-api/v1/chat-room/create → 200 OK")
    void createChatRoom_success() throws Exception {
        ChatRoomResponse resp = ChatRoomResponse.builder().id(10L).title("새 채팅").build();
        given(chatRoomService.createAiChatRoom(any(CurrentUserDto.class))).willReturn(resp);

        mvc.perform(post("/rest-api/v1/chat-room/create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("새 채팅"));
    }

    @Test
    @DisplayName("GET /rest-api/v1/chat-room/team/{teamId} → 200 OK")
    void getChatRoomByTeamId_success() throws Exception {
        Long teamId = 5L;
        ChatRoomResponse resp = ChatRoomResponse.builder().id(20L).title("Team Room").build();
        given(chatRoomService.findChatRoomByTeamId(eq(teamId), any(CurrentUserDto.class))).willReturn(resp);

        mvc.perform(get("/rest-api/v1/chat-room/team/{teamId}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20));
    }
}
