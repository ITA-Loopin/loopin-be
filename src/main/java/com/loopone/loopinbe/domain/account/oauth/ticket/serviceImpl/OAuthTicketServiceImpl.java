package com.loopone.loopinbe.domain.account.oauth.ticket.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.oauth.ticket.dto.OAuthTicketPayload;
import com.loopone.loopinbe.domain.account.oauth.ticket.service.OAuthTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthTicketServiceImpl implements OAuthTicketService {
    private static final String PREFIX = "oauth:ticket:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 티켓 생성
    @Override
    public String issue(OAuthTicketPayload payload, Duration ttl) {
        try {
            String ticket = UUID.randomUUID().toString().replace("-", "");
            String key = PREFIX + ticket;
            String value = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, value, ttl);
            return ticket;
        } catch (Exception e) {
            throw new RuntimeException("OAuth ticket 발급 실패", e);
        }
    }

    // 티켓 삭제
    @Override
    public OAuthTicketPayload consume(String ticket) {
        try {
            String key = PREFIX + ticket;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                throw new RuntimeException("유효하지 않거나 만료된 ticket 입니다.");
            }
            redisTemplate.delete(key); // 1회성
            return objectMapper.readValue(json, OAuthTicketPayload.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OAuth ticket 소비 실패", e);
        }
    }
}
