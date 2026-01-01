package com.loopone.loopinbe.domain.chat.chatroom.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.chat.chatRoom.controller.ChatRoomController;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomListResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ChatRoomController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        JwtAuthenticationFilter.class,     // ← 보안 필터 컴포넌트 제외
                        SecurityConfig.class,              // ← 전역 보안 설정 클래스를 쓰고 있다면 같이 제외
                        WebConfig.class                    // ← 전역 WebMvcConfigurer가 보안/리졸버를 끌어오면 제외
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
class ChatRoomControllerTest {
    @MockitoBean
    CurrentUserArgumentResolver currentUserArgumentResolver;
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatRoomController(chatRoomService))
                .setCustomArgumentResolvers(currentUserArgumentResolver)
                .build();

        given(currentUserArgumentResolver.supportsParameter(any()))
                .willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new CurrentUserDto(
                        1L, "jun@loop.in", null, "jun", "010-0000-0000",
                        Member.Gender.MALE, LocalDate.of(2000, 1, 1),
                        null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                        Member.OAuthProvider.GOOGLE, "provider-id"
                ));
    }

    @Test
    @DisplayName("채팅방 리스트 조회 API 테스트")
    void getChatRooms() throws Exception {

        ChatRoomListResponse fakeResponse =
                new ChatRoomListResponse(List.of());

        given(chatRoomService.getChatRooms(1L))
                .willReturn(fakeResponse);

        mockMvc.perform(get("/rest-api/v1/chat-room"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.chatRooms").exists());
    }
}
