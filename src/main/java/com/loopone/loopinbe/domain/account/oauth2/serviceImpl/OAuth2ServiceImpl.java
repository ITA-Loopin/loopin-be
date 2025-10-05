package com.loopone.loopinbe.domain.account.oauth2.serviceImpl;

import com.loopone.loopinbe.domain.account.member.dto.SocialUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.oauth2.dto.OAuth2WebPropertiesDto;
import com.loopone.loopinbe.domain.account.oauth2.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {
    private final RestTemplate restTemplate = new RestTemplate();
    private final OAuth2WebPropertiesDto oAuth2WebPropertiesDto;

    // 소셜 로그인 리디렉션 URL 생성
    @Override
    public String getAuthUrl(String provider) {
        Member.OAuthProvider p = Member.OAuthProvider.from(provider);
        OAuth2WebPropertiesDto.ProviderProperties props = propsOf(p);
        String base;
        String scope;
        switch (p) {
            case GOOGLE:
                base = "https://accounts.google.com/o/oauth2/v2/auth";
                scope = "openid%20profile%20email";
                break;
            case KAKAO:
                base = "https://kauth.kakao.com/oauth/authorize";
                scope = "account_email";
                break;
            case NAVER:
                base = "https://nid.naver.com/oauth2.0/authorize";
                scope = "email";
                break;
            default:
                throw new IllegalArgumentException("지원되지 않는 OAuth2 제공자: " + provider);
        }
        // 공통 파라미터 조립
        return base
                + "?client_id=" + props.clientId()
                + "&redirect_uri=" + props.redirectUri()
                + "&response_type=code"
                + "&scope=" + scope
                + (p == Member.OAuthProvider.NAVER ? "&state=" + generateState() : ""); // 네이버는 state 권장
    }

    private String generateState() {
        return UUID.randomUUID().toString(); // CSRF 방지용 랜덤 값
    }

    // 소셜 유저 정보 조회
    @Override
    public SocialUserDto getUserInfo(String provider, String code) {
        Member.OAuthProvider p = Member.OAuthProvider.from(provider);
        OAuth2WebPropertiesDto.ProviderProperties props = propsOf(p);

        String accessToken = getAccessToken(p, props, code);
        return getUserInfoFromProvider(p, props.userInfoUri(), accessToken);
    }

    // ----------------- 헬퍼 메서드 -----------------

    private OAuth2WebPropertiesDto.ProviderProperties propsOf(Member.OAuthProvider p) {
        OAuth2WebPropertiesDto.ProviderProperties props =
                oAuth2WebPropertiesDto.providers().get(p.key()); // google/kakao/naver
        if (props == null) throw new IllegalArgumentException("지원되지 않는 OAuth2 제공자: " + p);
        return props;
    }

    private String getAccessToken(Member.OAuthProvider provider,
                                  OAuth2WebPropertiesDto.ProviderProperties props,
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
        if (body == null || body.get("access_token") == null) {
            throw new RuntimeException("OAuth2 액세스 토큰을 가져올 수 없습니다.");
        }
        return (String) body.get("access_token");
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
