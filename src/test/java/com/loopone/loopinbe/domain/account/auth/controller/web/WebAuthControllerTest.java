package com.loopone.loopinbe.domain.account.auth.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.global.config.SecurityConfig;
import com.loopone.loopinbe.global.config.WebConfig;
import com.loopone.loopinbe.global.security.AuthCookieFactory;
import com.loopone.loopinbe.global.security.JwtAuthenticationFilter;
import com.loopone.loopinbe.global.security.TokenResolver;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WebAuthController.class,
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
class WebAuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean AuthCookieFactory authCookieFactory;
    @MockitoBean TokenResolver tokenResolver;
    @MockitoBean CurrentUserArgumentResolver currentUserArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(currentUserArgumentResolver.supportsParameter(any()))
                .willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new CurrentUserDto(
                        1L, "jun@loop.in", null, "jun", "010-0000-0000",
                        Member.Gender.MALE, LocalDate.of(2000,1,1),
                        null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                        Member.OAuthProvider.GOOGLE, "provider-id"
                ));
    }

    // ====== 회원가입 후 로그인 처리 ======
    @Test
    @DisplayName("POST /rest-api/v1/auth/signup-login → 200 OK & 토큰 쿠키 + LoginResponse 반환")
    void signUpAndLogin_success() throws Exception {
        // given
        MemberCreateRequest req = new MemberCreateRequest(
                "jun@loop.in",
                "jun",
                Member.OAuthProvider.GOOGLE,
                "provider-id"
        );

        LoginResponse loginResp = new LoginResponse(
                "access-token-123",
                "refresh-token-456"
        );

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", "access-cookie-value")
                .path("/")
                .httpOnly(true)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", "refresh-cookie-value")
                .path("/")
                .httpOnly(true)
                .build();

        given(authService.signUpAndLogin(any(MemberCreateRequest.class))).willReturn(loginResp);
        given(authCookieFactory.issueAccess("access-token-123")).willReturn(accessCookie);
        given(authCookieFactory.issueRefresh("refresh-token-456")).willReturn(refreshCookie);

        // when & then
        mvc.perform(post("/rest-api/v1/auth/signup-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) content().contentType(MediaType.APPLICATION_JSON))
                // ApiResponse<LoginResponse> 래퍼 안에 access/refresh 토큰이 들어오는지 검증
                .andExpect((ResultMatcher) jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect((ResultMatcher) jsonPath("$.data.refreshToken").value("refresh-token-456"))
                // 쿠키 두 개가 셋팅됐는지
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()
                ));

        verify(authService).signUpAndLogin(any(MemberCreateRequest.class));
    }

    // ====== 로그인 ======
    @Test
    @DisplayName("POST /rest-api/v1/auth/login → 200 OK & 토큰 쿠키 세팅")
    void login_success() throws Exception {
        // LoginRequest 구조는 실제 클래스에 맞게 수정해서 사용하면 됨
        // (예: email/password, email/providerId 등)
        class LoginRequestStub {
            public String email;
            public String password;

            public LoginRequestStub(String email, String password) {
                this.email = email;
                this.password = password;
            }
        }

        Object loginReqJson = new LoginRequestStub("jun@loop.in", "password");

        LoginResponse loginResp = new LoginResponse(
                "access-token-123",
                "refresh-token-456"
        );

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", "access-cookie-value")
                .path("/")
                .httpOnly(true)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", "refresh-cookie-value")
                .path("/")
                .httpOnly(true)
                .build();

        given(authService.login(any())).willReturn(loginResp);
        given(authCookieFactory.issueAccess("access-token-123")).willReturn(accessCookie);
        given(authCookieFactory.issueRefresh("refresh-token-456")).willReturn(refreshCookie);

        mvc.perform(post("/rest-api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqJson)))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) content().contentType(MediaType.APPLICATION_JSON))
                // body는 ApiResponse<Void> 라 data 검증은 생략
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()
                ));

        verify(authService).login(any());
    }

    // ====== 로그아웃 ======
    @Test
    @DisplayName("POST /rest-api/v1/auth/logout → 200 OK & 만료 쿠키 세팅 + 서비스 호출")
    void logout_success() throws Exception {
        // given
        given(tokenResolver.resolveAccess(any(HttpServletRequest.class)))
                .willReturn("access-token-123");

        ResponseCookie expiredAccess = ResponseCookie.from("ACCESS", "")
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie expiredRefresh = ResponseCookie.from("REFRESH", "")
                .path("/")
                .maxAge(0)
                .build();

        given(authCookieFactory.expireAccess()).willReturn(expiredAccess);
        given(authCookieFactory.expireRefresh()).willReturn(expiredRefresh);

        // when & then
        mvc.perform(post("/rest-api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // ACCESS 쿠키 만료 검증
                .andExpect(cookie().value("ACCESS", ""))
                .andExpect(cookie().maxAge("ACCESS", 0))
                // REFRESH 쿠키 만료 검증
                .andExpect(cookie().value("REFRESH", ""))
                .andExpect(cookie().maxAge("REFRESH", 0));

        verify(authService).logout(any(CurrentUserDto.class), eq("access-token-123"));
    }

    // ====== accessToken 재발급 ======
    @Test
    @DisplayName("GET /rest-api/v1/auth/refresh-token → 200 OK & 새 access 쿠키 세팅")
    void refreshToken_success() throws Exception {
        // given
        given(tokenResolver.resolveRefresh(any(HttpServletRequest.class)))
                .willReturn("refresh-token-456");

        LoginResponse refreshed = new LoginResponse(
                "new-access-token",
                "refresh-token-456"
        );

        ResponseCookie newAccessCookie = ResponseCookie.from("ACCESS", "new-access-cookie")
                .path("/")
                .httpOnly(true)
                .build();

        given(authService.refreshToken(eq("refresh-token-456"), any(CurrentUserDto.class)))
                .willReturn(refreshed);
        given(authCookieFactory.issueAccess("new-access-token"))
                .willReturn(newAccessCookie);

        // when & then
        mvc.perform(get("/rest-api/v1/auth/refresh-token"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) content().contentType(MediaType.APPLICATION_JSON))
                // refresh-token 엔드포인트는 access 쿠키만 재발급
                .andExpect(header().string(HttpHeaders.SET_COOKIE, newAccessCookie.toString()));

        verify(authService).refreshToken(eq("refresh-token-456"), any(CurrentUserDto.class));
    }
}

