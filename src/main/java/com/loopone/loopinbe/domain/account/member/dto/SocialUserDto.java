package com.loopone.loopinbe.domain.account.member.dto;

import com.loopone.loopinbe.domain.account.member.entity.Member;

public record SocialUserDto(String email, Member.OAuthProvider provider, String providerId) {}
