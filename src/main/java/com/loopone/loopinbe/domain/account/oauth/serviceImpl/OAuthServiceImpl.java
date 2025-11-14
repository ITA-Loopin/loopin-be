package com.loopone.loopinbe.domain.account.oauth.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.dto.req.LoginRequest;
import com.loopone.loopinbe.domain.account.auth.dto.res.LoginResponse;
import com.loopone.loopinbe.domain.account.auth.service.AuthService;
import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.oauth.dto.res.OAuthRedirectResponse;
import com.loopone.loopinbe.global.config.properties.OAuthWebProperties;
import com.loopone.loopinbe.domain.account.oauth.enums.FrontendEnv;
import com.loopone.loopinbe.domain.account.oauth.service.OAuthService;
import com.loopone.loopinbe.domain.account.oauth.service.OAuthStateService;
import com.loopone.loopinbe.global.config.properties.FrontendRedirectProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final OAuthWebProperties oAuthWebProperties;
    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final OAuthStateService stateService;
    private final FrontendRedirectProperties frontendRedirectProperties;
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";

    // 소셜 로그인 리디렉션 URL 생성
    @Override
    public String getAuthUrl(String provider, FrontendEnv env) {
        Member.OAuthProvider p = Member.OAuthProvider.from(provider);
        OAuthWebProperties.ProviderProperties props = propsOf(p);
        String base;
        List<String> scopes;
        switch (p) {
            case GOOGLE -> {
                base = "https://accounts.google.com/o/oauth2/v2/auth";
                scopes = List.of("openid", "profile", "email");
            }
            case KAKAO -> {
                base = "https://kauth.kakao.com/oauth/authorize";
                scopes = List.of("account_email");
            }
            case NAVER -> {
                base = "https://nid.naver.com/oauth2.0/authorize";
                scopes = List.of("email");
            }
            default -> throw new IllegalArgumentException("지원되지 않는 OAuth2 제공자: " + provider);
        }
        // env가 들어간 서명된 state 발급 (5분)
        String state = stateService.issue(env, Duration.ofMinutes(5));

        // 안전한 파라미터 인코딩
        return UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("state", state) // 모든 제공자에 state 사용
                .build()
                .toUriString();
    }

    // OAuth state 토큰 검증
    @Override
    public FrontendEnv resolveEnvFromState(String state) {
        return stateService.consume(state);
    }

    // 소셜 유저 정보 조회
    @Override
    public SocialUserDto getUserInfo(String provider, String code) {
        Member.OAuthProvider p = Member.OAuthProvider.from(provider);
        OAuthWebProperties.ProviderProperties props = propsOf(p);

        String accessToken = getAccessToken(p, props, code);
        return getUserInfoFromProvider(p, props.userInfoUri(), accessToken);
    }

    // 리디렉션 URL 생성 (env 별 분기)
    @Override
    public OAuthRedirectResponse buildRedirectResponse(SocialUserDto socialUser, FrontendEnv env) {
        String base = frontendRedirectProperties.urlFor(env);
        String email = socialUser.email();
        boolean existing = memberRepository.existsByEmail(email);

        if (existing) {
            // 내부 로그인
            LoginResponse login = authService.login(LoginRequest.builder().email(email).build());
            String redirectUrl = UriComponentsBuilder.fromUriString(base)
                    .queryParam("status", "LOGIN_SUCCESS")
                    .queryParam(ACCESS_TOKEN,  login.getAccessToken())
                    .queryParam(REFRESH_TOKEN,  login.getRefreshToken())
                    .build()
                    .toUriString();
            return new OAuthRedirectResponse(true, redirectUrl, login.getAccessToken(), login.getRefreshToken());
        } else {
            String redirectUrl = UriComponentsBuilder.fromUriString(base)
                    .queryParam("status", "SIGNUP_REQUIRED")
                    .queryParam("email", email)
                    .queryParam("provider", socialUser.provider().name())
                    .queryParam("providerId", socialUser.providerId())
                    .build()
                    .toUriString();
            return new OAuthRedirectResponse(false, redirectUrl, null, null);
        }
    }

    // ----------------- 헬퍼 메서드 -----------------

    private OAuthWebProperties.ProviderProperties propsOf(Member.OAuthProvider p) {
        OAuthWebProperties.ProviderProperties props =
                oAuthWebProperties.providers().get(p.key()); // google/kakao/naver
        if (props == null) throw new IllegalArgumentException("지원되지 않는 OAuth2 제공자: " + p);
        return props;
    }

    private String getAccessToken(Member.OAuthProvider provider,
                                  OAuthWebProperties.ProviderProperties props,
                                  String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", props.clientId());
        params.add("redirect_uri", props.redirectUri());
        params.add("grant_type", "authorization_code");

        // Kakao는 client_secret 미사용(설정 시에도 종종 미검증)
        if (!provider.isKakao() && props.clientSecret() != null) {
            params.add("client_secret", props.clientSecret());
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                props.tokenUri(), HttpMethod.POST, request, Map.class
        );
        Map body = response.getBody();
        if (body == null || body.get(ACCESS_TOKEN) == null) {
            throw new RuntimeException("OAuth2 액세스 토큰을 가져올 수 없습니다.");
        }
        return (String) body.get(ACCESS_TOKEN);
    }

    @SuppressWarnings("unchecked")
    private SocialUserDto getUserInfoFromProvider(Member.OAuthProvider provider,
                                                  String userInfoUri,
                                                  String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        if (provider.isKakao()) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, entity, Map.class
        );
        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("OAuth2 사용자 정보를 가져올 수 없습니다.");

        String email = null;
        String providerId = null;

        switch (provider) {
            case GOOGLE:
                // OpenID 표준 claims
                email = (String) body.get("email");
                providerId = (String) body.get("sub");
                break;
            case KAKAO:
                Object idObj = body.get("id");
                providerId = idObj != null ? String.valueOf(idObj) : null;
                Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }
                break;
            case NAVER:
                Map<String, Object> naverResp = (Map<String, Object>) body.get("response");
                if (naverResp != null) {
                    providerId = (String) naverResp.get("id");
                    email = (String) naverResp.get("email");
                }
                break;
            default:
        }
        if (providerId == null || providerId.isBlank()) {
            throw new RuntimeException("OAuth2 providerId를 확인할 수 없습니다.");
        }
        if (email == null || email.isBlank()) {
            // 이메일 비공개 시 대체 이메일 구성
            email = provider.name().toLowerCase() + "_" + providerId + "@example.com";
        }
        return new SocialUserDto(email, provider, providerId);
    }
}
