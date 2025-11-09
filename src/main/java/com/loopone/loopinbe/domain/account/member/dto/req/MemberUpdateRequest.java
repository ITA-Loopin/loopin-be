package com.loopone.loopinbe.domain.account.member.dto.req;

import jakarta.validation.constraints.Size;

public record MemberUpdateRequest (
        String email,
//        String password,
        @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
        String nickname
//        String phone,
//        Member.Gender gender,
//        LocalDate birthday
) {}
