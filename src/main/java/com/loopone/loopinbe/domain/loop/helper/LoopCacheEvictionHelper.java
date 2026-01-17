package com.loopone.loopinbe.domain.loop.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoopCacheEvictionHelper {

    private final CacheManager cacheManager;

    // 트랜잭션 커밋 이후 관련 캐시 무효화 (롤백 시 유지)
    public void evictAfterCommit(
            Long memberId,
            Collection<Long> loopIds,
            Collection<LocalDate> dates,
            Collection<YearMonth> yearMonths
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션이 없으면 즉시 무효화
            evictNow(memberId, loopIds, dates, yearMonths);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictNow(memberId, loopIds, dates, yearMonths);
            }
        });
    }

    // 실제 캐시 무효화 로직
    public void evictNow(
            Long memberId,
            Collection<Long> loopIds,
            Collection<LocalDate> dates,
            Collection<YearMonth> yearMonths
    ) {
        // 1) loopReport
        Cache reportCache = cacheManager.getCache("loopReport");
        if (reportCache != null) {
            reportCache.evictIfPresent(memberId);
            log.debug("LoopReport cache evicted: {}", memberId);
        }
        // 2) loopDetail (key: memberId:loopId)
        Cache detailCache = cacheManager.getCache("loopDetail");
        if (detailCache != null && loopIds != null) {
            for (Long loopId : loopIds) {
                if (loopId == null) continue;
                detailCache.evictIfPresent(detailKey(memberId, loopId));
            }
        }
        // 3) dailyLoops (key: memberId:date)
        Cache dailyCache = cacheManager.getCache("dailyLoops");
        if (dailyCache != null && dates != null) {
            for (LocalDate d : dates) {
                if (d == null) continue;
                dailyCache.evictIfPresent(dailyKey(memberId, d));
            }
        }
        // 4) loopCalendar (key: memberId:year:month)
        Cache calendarCache = cacheManager.getCache("loopCalendar");
        if (calendarCache != null && yearMonths != null) {
            for (YearMonth ym : yearMonths) {
                if (ym == null) continue;
                calendarCache.evictIfPresent(calendarKey(memberId, ym));
            }
        }
    }

    private String detailKey(Long memberId, Long loopId) {
        return memberId + ":" + loopId;
    }

    private String dailyKey(Long memberId, LocalDate date) {
        return memberId + ":" + date;
    }

    private String calendarKey(Long memberId, YearMonth ym) {
        return memberId + ":" + ym.getYear() + ":" + ym.getMonthValue();
    }
}
