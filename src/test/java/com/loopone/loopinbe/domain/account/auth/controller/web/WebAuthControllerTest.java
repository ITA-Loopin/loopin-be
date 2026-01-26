package com.loopone.loopinbe.domain.account.auth.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.req.OAuthLoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.member.entity.Member;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WebAuthControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    // ===== controller deps =====
    @MockitoBean AuthService authService;
    @MockitoBean WebAuthCookieFactory webAuthCookieFactory;
    @MockitoBean TokenResolver tokenResolver;

    // ===== for @CurrentUser =====
    @MockitoBean CurrentUserArgumentResolver currentUserArgumentResolver;
    private CurrentUserDto currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new CurrentUserDto(
                1L, "jun@loop.in", null, "jun", "010-0000-0000",
                Member.Gender.MALE, LocalDate.of(2000, 1, 1),
                null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.GOOGLE, "provider-id"
        );
        given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(currentUser);
    }

    @TestConfiguration
    static class WebMvcTestConfig implements WebMvcConfigurer {
        private final CurrentUserArgumentResolver currentUserArgumentResolver;

        WebMvcTestConfig(CurrentUserArgumentResolver resolver) {
            this.currentUserArgumentResolver = resolver;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(currentUserArgumentResolver);
        }
    }

    @Test
    @DisplayName("POST /rest-api/v1/auth/signup-login → 200 OK & Set-Cookie 2개")
    void signUpAndLogin_success() throws Exception {
        // given
        var req = new OAuthLoginRequest("jun", "ticket-123");
        var loginResp = new LoginResponse("access-token-aaa", "refresh-token-bbb");

        given(authService.signUpAndLogin(eq("jun"), eq("ticket-123"))).willReturn(loginResp);
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", "access-token-aaa").path("/").httpOnly(true).build();
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", "refresh-token-bbb").path("/").httpOnly(true).build();
        given(webAuthCookieFactory.issueAccess("access-token-aaa")).willReturn(accessCookie);
        given(webAuthCookieFactory.issueRefresh("refresh-token-bbb")).willReturn(refreshCookie);

        // when & then
        mvc.perform(post("/rest-api/v1/auth/signup-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()))
                .andExpect((ResultMatcher) content().contentType(MediaType.APPLICATION_JSON))
                // ApiResponse.success() 형태에 맞게 필요하면 아래를 조정
                .andExpect((ResultMatcher) jsonPath("$.data").doesNotExist());
        then(authService).should().signUpAndLogin("jun", "ticket-123");
    }

    @Test
    @DisplayName("POST /rest-api/v1/auth/login → 200 OK & 쿠키 만료 2개")
    void login_success() throws Exception {
        // given
        var req = new LoginRequest("jun@loop.in");
        var loginResp = new LoginResponse("access-token-aaa", "refresh-token-bbb");

        given(authService.login(any(LoginRequest.class))).willReturn(loginResp);
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS", "access-token-aaa").path("/").httpOnly(true).build();
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", "refresh-token-bbb").path("/").httpOnly(true).build();
        given(webAuthCookieFactory.issueAccess("access-token-aaa")).willReturn(accessCookie);
        given(webAuthCookieFactory.issueRefresh("refresh-token-bbb")).willReturn(refreshCookie);

        // when & then
        mvc.perform(post("/rest-api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").doesNotExist());
        then(authService).should().login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /rest-api/v1/auth/logout → 200 OK & 쿠키 만료 2개")
    void logout_success() throws Exception {
        // given
        given(tokenResolver.resolveAccess(any())).willReturn("access-token-aaa");
        ResponseCookie expiredAccess = ResponseCookie.from("ACCESS", "").maxAge(0).path("/").httpOnly(true).build();
        ResponseCookie expiredRefresh = ResponseCookie.from("REFRESH", "").maxAge(0).path("/").httpOnly(true).build();
        given(webAuthCookieFactory.expireAccess()).willReturn(expiredAccess);
        given(webAuthCookieFactory.expireRefresh()).willReturn(expiredRefresh);

        // when & then
        mvc.perform(post("/rest-api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(result -> {
                    var cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(cookies).hasSize(2);
                    assertThat(cookies).anySatisfy(v -> {
                        assertThat(v).contains("ACCESS=");
                        assertThat(v).contains("Max-Age=0");
                        assertThat(v).contains("Path=/");
                        assertThat(v).contains("HttpOnly");
                    });
                    assertThat(cookies).anySatisfy(v -> {
                        assertThat(v).contains("REFRESH=");
                        assertThat(v).contains("Max-Age=0");
                        assertThat(v).contains("Path=/");
                        assertThat(v).contains("HttpOnly");
                    });
                });
        then(authService).should().logout(eq(currentUser), eq("access-token-aaa"));
    }

    @Test
    @DisplayName("GET /rest-api/v1/auth/refresh-token → 200 OK & access 쿠키만 갱신")
    void refreshToken_success() throws Exception {
        // given
        given(tokenResolver.resolveRefresh(any())).willReturn("refresh-token-bbb");
        given(authService.refreshToken("refresh-token-bbb"))
                .willReturn(new LoginResponse("new-access-ccc", "refresh-token-bbb"));
        ResponseCookie newAccessCookie = ResponseCookie.from("ACCESS", "new-access-ccc").path("/").httpOnly(true).build();
        given(webAuthCookieFactory.issueAccess("new-access-ccc")).willReturn(newAccessCookie);

        // when & then
        mvc.perform(get("/rest-api/v1/auth/refresh-token"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, newAccessCookie.toString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").doesNotExist());
        then(authService).should().refreshToken("refresh-token-bbb");
    }
}
