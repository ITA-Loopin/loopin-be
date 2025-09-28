package com.loopone.loopinbe.global.redis.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ConvertingCursor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCacheEvictHelper {
    private final RedisTemplate<String, Object> redisTemplate;

    // 특정 prefix로 시작하는 캐시 키를 모두 삭제
    public void evictByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(1000)
                .build();
        try (Cursor<String> cursor = redisTemplate.executeWithStickyConnection(connection ->
                new ConvertingCursor<>(connection.scan(options), new StringRedisSerializer()::deserialize))) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                redisTemplate.delete(key);
            }
        }
    }
}
