package com.loopone.loopinbe.domain.account.auth.currentUser;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CurrentUserDto(
        Long id,
        String email,
        String password,
        String nickname,
        String phone,
        Member.Gender gender,
        LocalDate birthday,
        String profileImageUrl,
        Member.State state,
        Member.MemberRole role,
        Member.OAuthProvider provider,
        String providerId
) {}
