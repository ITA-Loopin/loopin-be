package com.loopone.loopinbe.domain.account.member.dto.req;

import com.loopone.loopinbe.domain.account.member.entity.Member;

import java.time.LocalDate;

public record MemberUpdateRequest (
        String email,
//        String password,
        String nickname
//        String phone,
//        Member.Gender gender,
//        LocalDate birthday
) {}
