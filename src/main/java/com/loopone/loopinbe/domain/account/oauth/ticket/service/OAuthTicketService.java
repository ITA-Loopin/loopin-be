package com.loopone.loopinbe.domain.account.oauth.ticket.service;

import com.loopone.loopinbe.domain.account.oauth.ticket.dto.OAuthTicketPayload;

import java.time.Duration;

public interface OAuthTicketService {
    // 티켓 생성
    String issue(OAuthTicketPayload payload, Duration ttl);

    // 티켓 삭제
    OAuthTicketPayload consume(String ticket);
}
