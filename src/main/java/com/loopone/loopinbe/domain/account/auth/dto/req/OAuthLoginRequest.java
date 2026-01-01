package com.loopone.loopinbe.domain.account.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
        String nickname,

        @NotBlank(message = "ticket은 필수입니다.")
        String ticket
) {}
