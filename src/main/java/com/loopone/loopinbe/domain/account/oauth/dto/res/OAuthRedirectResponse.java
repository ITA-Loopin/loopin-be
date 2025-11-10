package com.loopone.loopinbe.domain.account.oauth.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthRedirectResponse {
    private final boolean loginSuccess;
    private final String redirectUrl; // FE로 보낼 URL (쿼리엔 토큰 노출 지양)
    private final String accessToken; // 쿠키 생성용 (컨트롤러에서만 사용)
    private final String refreshToken; // 쿠키 생성용 (컨트롤러에서만 사용)
}
