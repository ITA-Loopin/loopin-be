package com.loopone.loopinbe.domain.account.auth.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.security.JwtTokenProvider;
import com.loopone.loopinbe.domain.account.auth.service.AccessTokenDenyListService;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.auth.service.RefreshTokenService;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.webSocket.util.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberService memberService;
    private final WsSessionRegistry wsSessionRegistry;
    private final AccessTokenDenyListService accessTokenDenyListService;

    @Value("${custom.accessToken.expiration}")
    private Duration accessTokenExpiration;

    @Value("${custom.refreshToken.expiration}")
    private Duration refreshTokenExpiration;

    // 회원가입 후 로그인 처리
    @Override
    @Transactional
    public LoginResponse signUpAndLogin(MemberCreateRequest memberCreateRequest) {
        Member newMember = memberService.regularSignUp(memberCreateRequest);
        // 회원가입 직후 로그인 처리
        LoginRequest loginRequest = LoginRequest.builder()
                .email(newMember.getEmail())
                .build();
        return login(loginRequest); // 기존 로그인 로직 재사용
    }

    // 로그인
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        String accessToken = jwtTokenProvider.generateToken(member.getEmail(), "ACCESS",accessTokenExpiration);
        String refreshToken = jwtTokenProvider.generateToken(member.getEmail(), "REFRESH",refreshTokenExpiration);
        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(member.getId().toString(), refreshToken, refreshTokenExpiration);
        return new LoginResponse(accessToken, refreshToken);
    }

    // 로그아웃
    @Override
    public void logout(CurrentUserDto currentUser, String accessToken) {
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(currentUser.id().toString());
        // access 즉시 차단(deny-list)
        if (accessToken != null && jwtTokenProvider.validateAccessToken(accessToken)) {
            String jti = jwtTokenProvider.getJti(accessToken);
            long ttlSec = jwtTokenProvider.getRemainingSeconds(accessToken);
            if (ttlSec > 0) {
                accessTokenDenyListService.deny(jti, Duration.ofSeconds(ttlSec));
            }
        }
        // WS 모두 종료 (4401: Unauthorized/Logged out)
        wsSessionRegistry.closeAll(currentUser.id(), new CloseStatus(4401, "Logged out"));
        log.info("Logout: memberId={}, closedWsSessions={}", currentUser.id(),
                wsSessionRegistry.count(currentUser.id()));
    }

    // accessToken 재발급
    @Override
    public LoginResponse refreshToken(String refreshToken, CurrentUserDto currentUser) {
        // "Bearer "가 붙어있다면 제거
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        String storedRefreshToken = refreshTokenService.getRefreshToken(currentUser.id().toString());
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }
        if (!jwtTokenProvider.validateRefreshToken(storedRefreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }
        String email = jwtTokenProvider.getEmailFromToken(storedRefreshToken);
        String newAccessToken = jwtTokenProvider.generateToken(email, "ACCESS", accessTokenExpiration);

        return new LoginResponse(newAccessToken, storedRefreshToken);
    }
}
