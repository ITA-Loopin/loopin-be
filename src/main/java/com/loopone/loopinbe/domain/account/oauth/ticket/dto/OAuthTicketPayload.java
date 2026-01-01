package com.loopone.loopinbe.domain.account.oauth.ticket.dto;

import com.loopone.loopinbe.domain.account.member.entity.Member;

public record OAuthTicketPayload(
        String email,
        Member.OAuthProvider provider,
        String providerId
) {}
