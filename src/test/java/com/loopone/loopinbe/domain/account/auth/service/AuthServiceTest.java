package com.loopone.loopinbe.domain.account.auth.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.serviceImpl.AuthServiceImpl;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverterImpl;
import com.loopone.loopinbe.domain.account.member.converter.SimpleMemberConverterImpl;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowReqRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.domain.account.member.serviceImpl.MemberServiceImpl;
import com.loopone.loopinbe.domain.account.oauth.ticket.dto.OAuthTicketPayload;
import com.loopone.loopinbe.domain.account.oauth.ticket.service.OAuthTicketService;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.auth.AuthEventPublisher;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
import com.loopone.loopinbe.global.s3.S3Service;
import com.loopone.loopinbe.global.security.JwtTokenProvider;
import com.loopone.loopinbe.global.webSocket.util.WsSessionRegistry;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "custom.accessToken.expiration=30m",
        "custom.refreshToken.expiration=30d"
})
@Import({
        TestContainersConfig.class,
        AuthServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthServiceTest {
    // ===== Real Repositories =====
    @Autowired MemberRepository memberRepository;
    @Autowired MemberFollowReqRepository memberFollowReqRepository;
    @Autowired MemberFollowRepository memberFollowRepository;

    // ===== SUT =====
    @Autowired AuthServiceImpl authService;

    // ===== External boundaries (mock) =====
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean RefreshTokenService refreshTokenService;
    @MockitoBean AccessTokenDenyListService accessTokenDenyListService;
    @MockitoBean WsSessionRegistry wsSessionRegistry;
    @MockitoBean OAuthTicketService oAuthTicketService;

    // MemberServiceImpl이 필요로 하는 외부 경계들 (MemberServiceTest와 동일)
    @MockitoBean S3Service s3Service;
    @MockitoBean MemberService memberService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean TeamService teamService;
    @MockitoBean LoopService loopService;
    @MockitoBean NotificationEventPublisher notificationEventPublisher;
    @MockitoBean AuthEventPublisher authEventPublisher;

    @AfterEach
    void cleanup() {
        memberFollowRepository.deleteAll();
        memberFollowReqRepository.deleteAll();
        memberRepository.deleteAll();
        // refreshTokenService 등은 mock이라 cleanup 불필요
    }

    // ===== Helpers =====
    private Member persistMember(String email, String nickname) {
        Member m = Member.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(null)
                .state(Member.State.NORMAL)
                .role(Member.MemberRole.ROLE_USER)
                .oAuthProvider(Member.OAuthProvider.GOOGLE)
                .providerId("pid")
                .build();
        return memberRepository.saveAndFlush(m);
    }

    private CurrentUserDto cu(Member m) {
        return new CurrentUserDto(
                m.getId(),
                m.getEmail(),
                null,
                m.getNickname(),
                null,
                null,
                null,
                m.getProfileImageUrl(),
                m.getState(),
                m.getRole(),
                m.getOAuthProvider(),
                m.getProviderId()
        );
    }

    // =========================================================
    // login
    // =========================================================
    @Nested
    class Login {

        @Test
        @DisplayName("성공: 이메일로 멤버 조회 후 Access/Refresh 토큰 발급 + RefreshToken Redis 저장(email key)")
        void success() {
            // given
            String email = "jun@loop.in";
            persistMember(email, "jun");
            given(jwtTokenProvider.generateToken(eq(email), eq("ACCESS"), any(Duration.class)))
                    .willReturn("access-token-123");
            given(jwtTokenProvider.generateToken(eq(email), eq("REFRESH"), any(Duration.class)))
                    .willReturn("refresh-token-456");
            LoginRequest req = LoginRequest.builder()
                    .email(email)
                    .build();

            // when
            LoginResponse resp = authService.login(req);

            // then
            assertThat(resp.getAccessToken()).isEqualTo("access-token-123");
            assertThat(resp.getRefreshToken()).isEqualTo("refresh-token-456");
            verify(jwtTokenProvider).generateToken(eq(email), eq("ACCESS"), any(Duration.class));
            verify(jwtTokenProvider).generateToken(eq(email), eq("REFRESH"), any(Duration.class));
            verify(refreshTokenService).saveRefreshToken(eq(email), eq("refresh-token-456"), any(Duration.class));
        }

        @Test
        @DisplayName("실패: 멤버가 없으면 USER_NOT_FOUND")
        void userNotFound() {
            // given
            String email = "no@loop.in";
            LoginRequest req = LoginRequest.builder().email(email).build();

            // when & then
            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ReturnCode.USER_NOT_FOUND.getMessage());

            verify(jwtTokenProvider, never()).generateToken(anyString(), anyString(), any(Duration.class));
            verify(refreshTokenService, never()).saveRefreshToken(anyString(), anyString(), any(Duration.class));
        }
    }

    // =========================================================
    // signUpAndLogin
    // =========================================================
    @Nested
    class SignUpAndLogin {

        @Test
        @DisplayName("성공: ticket consume -> regularSignUp 저장 -> login 재사용해서 토큰 발급 + refresh 저장")
        void success() {
            // given
            String ticket = "ticket-abc";
            String email = "new@loop.in";
            String nickname = "newNick";
            OAuthTicketPayload payload = mock(OAuthTicketPayload.class);
            given(payload.email()).willReturn(email);
            given(payload.provider()).willReturn(Member.OAuthProvider.GOOGLE);
            given(payload.providerId()).willReturn("pid-x");
            given(oAuthTicketService.consume(ticket)).willReturn(payload);
            given(jwtTokenProvider.generateToken(eq(email), eq("ACCESS"), any(Duration.class))).willReturn("access-token-123");
            given(jwtTokenProvider.generateToken(eq(email), eq("REFRESH"), any(Duration.class))).willReturn("refresh-token-456");
            given(memberService.regularSignUp(any(MemberCreateRequest.class)))
                    .willAnswer(inv -> {
                        MemberCreateRequest req = inv.getArgument(0, MemberCreateRequest.class);

                        Member m = Member.builder()
                                .email(req.getEmail())
                                .nickname(req.getNickname())
                                .profileImageUrl(null)
                                .state(Member.State.NORMAL)
                                .role(Member.MemberRole.ROLE_USER)
                                .oAuthProvider(req.getProvider())
                                .providerId(req.getProviderId())
                                .build();

                        return memberRepository.saveAndFlush(m);
                    });

            // when
            LoginResponse resp = authService.signUpAndLogin(nickname, ticket);

            // then
            assertThat(resp.getAccessToken()).isEqualTo("access-token-123");
            assertThat(resp.getRefreshToken()).isEqualTo("refresh-token-456");

            // 회원이 실제 저장됐는지
            verify(memberService).regularSignUp(argThat(req ->
                    req.getEmail().equals(email)
                            && req.getNickname().equals(nickname)
                            && req.getProvider() == Member.OAuthProvider.GOOGLE
                            && req.getProviderId().equals("pid-x")
            ));
            verify(oAuthTicketService).consume(ticket);
            verify(refreshTokenService).saveRefreshToken(eq(email), eq("refresh-token-456"), any(Duration.class));
        }
    }

    // =========================================================
    // logout
    // =========================================================
    @Nested
    class Logout {

        @Test
        @DisplayName("성공: RefreshToken 삭제(email) + access deny-list 등록 + WS 세션 종료")
        void success() {
            // given
            Member m = persistMember("jun@loop.in", "jun");
            CurrentUserDto currentUser = cu(m);
            String accessToken = "access-token-123";
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(true);
            given(jwtTokenProvider.getJti(accessToken)).willReturn("jti-123");
            given(jwtTokenProvider.getRemainingSeconds(accessToken)).willReturn(3600L);

            // when
            authService.logout(currentUser, accessToken);

            // then
            verify(refreshTokenService).deleteRefreshToken(currentUser.email());
            verify(jwtTokenProvider).validateAccessToken(accessToken);
            verify(jwtTokenProvider).getJti(accessToken);
            verify(jwtTokenProvider).getRemainingSeconds(accessToken);
            verify(accessTokenDenyListService).deny(eq("jti-123"), eq(Duration.ofSeconds(3600L)));
            verify(wsSessionRegistry).closeAll(eq(currentUser.id()),
                    argThat(status -> status.getCode() == 4401));
        }

        @Test
        @DisplayName("accessToken이 null이면 deny-list 로직은 타지 않음")
        void accessTokenNull() {
            // given
            Member m = persistMember("jun@loop.in", "jun");
            CurrentUserDto currentUser = cu(m);

            // when
            authService.logout(currentUser, null);

            // then
            verify(refreshTokenService).deleteRefreshToken(currentUser.email());
            verify(jwtTokenProvider, never()).validateAccessToken(anyString());
            verify(accessTokenDenyListService, never()).deny(anyString(), any(Duration.class));
            verify(wsSessionRegistry).closeAll(eq(currentUser.id()), argThat(status -> status.getCode() == 4401));
        }

        @Test
        @DisplayName("accessToken이 유효하지 않으면 deny-list에 추가하지 않음")
        void invalidAccessToken() {
            // given
            Member m = persistMember("jun@loop.in", "jun");
            CurrentUserDto currentUser = cu(m);
            String accessToken = "invalid-token";
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(false);

            // when
            authService.logout(currentUser, accessToken);

            // then
            verify(refreshTokenService).deleteRefreshToken(currentUser.email());
            verify(jwtTokenProvider).validateAccessToken(accessToken);
            verify(accessTokenDenyListService, never()).deny(anyString(), any(Duration.class));
            verify(wsSessionRegistry).closeAll(eq(currentUser.id()), argThat(status -> status.getCode() == 4401));
        }
    }

    // =========================================================
    // refreshToken
    // =========================================================
    @Nested
    class RefreshToken {

        @Test
        @DisplayName("성공: Bearer 제거 -> validate -> email 추출 -> 저장된 토큰과 일치 -> 새 Access 발급")
        void success() {
            // given
            String raw = "refresh-token-456";
            String bearer = "Bearer " + raw;
            String email = "jun@loop.in";
            given(jwtTokenProvider.validateRefreshToken(raw)).willReturn(true);
            given(jwtTokenProvider.getEmailFromToken(raw)).willReturn(email);
            given(refreshTokenService.getRefreshToken(email)).willReturn(raw);
            given(jwtTokenProvider.generateToken(eq(email), eq("ACCESS"), any(Duration.class))).willReturn("new-access-token");

            // when
            LoginResponse resp = authService.refreshToken(bearer);

            // then
            assertThat(resp.getAccessToken()).isEqualTo("new-access-token");
            assertThat(resp.getRefreshToken()).isEqualTo(raw);
            verify(jwtTokenProvider, times(2)).validateRefreshToken(raw); // 2번 호출이 맞음
            verify(jwtTokenProvider).getEmailFromToken(raw);
            verify(refreshTokenService).getRefreshToken(email);
            verify(jwtTokenProvider).generateToken(eq(email), eq("ACCESS"), any(Duration.class));
        }

        @Test
        @DisplayName("실패: refreshToken이 null/blank면 예외")
        void missing() {
            assertThatThrownBy(() -> authService.refreshToken("  "))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("리프레시 토큰이 없습니다.");
        }

        @Test
        @DisplayName("실패: 1차 validateRefreshToken 실패면 예외")
        void invalidOrExpiredAtFirstValidation() {
            String raw = "refresh-token-456";
            given(jwtTokenProvider.validateRefreshToken(raw)).willReturn(false);
            assertThatThrownBy(() -> authService.refreshToken(raw))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("만료되었거나 유효하지 않습니다.");

            verify(jwtTokenProvider).validateRefreshToken(raw);
            verify(refreshTokenService, never()).getRefreshToken(anyString());
        }

        @Test
        @DisplayName("실패: 저장된 토큰이 없거나 값이 다르면 예외")
        void notMatched() {
            String raw = "refresh-token-456";
            String email = "jun@loop.in";
            given(jwtTokenProvider.validateRefreshToken(raw)).willReturn(true);
            given(jwtTokenProvider.getEmailFromToken(raw)).willReturn(email);
            given(refreshTokenService.getRefreshToken(email)).willReturn("other-token");
            assertThatThrownBy(() -> authService.refreshToken(raw))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 리프레시 토큰입니다.");

            verify(jwtTokenProvider).validateRefreshToken(raw);
            verify(jwtTokenProvider).getEmailFromToken(raw);
            verify(refreshTokenService).getRefreshToken(email);
            verify(jwtTokenProvider, never()).generateToken(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("실패: 저장된 refreshToken이 2차 validate에서 만료면 예외")
        void expiredAtSecondValidation() {
            String raw = "refresh-token-456";
            String email = "jun@loop.in";
            given(jwtTokenProvider.validateRefreshToken(raw)).willReturn(true);
            given(jwtTokenProvider.getEmailFromToken(raw)).willReturn(email);
            given(refreshTokenService.getRefreshToken(email)).willReturn(raw);
            // 2번째 validate에서 false
            given(jwtTokenProvider.validateRefreshToken(raw))
                    .willReturn(true)   // 1차
                    .willReturn(false); // 2차
            assertThatThrownBy(() -> authService.refreshToken(raw))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("리프레시 토큰이 만료되었습니다.");

            verify(jwtTokenProvider, never()).generateToken(anyString(), anyString(), any(Duration.class));
        }
    }
}
