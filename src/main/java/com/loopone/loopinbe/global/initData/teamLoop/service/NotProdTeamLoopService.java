package com.loopone.loopinbe.global.initData.teamLoop.service;

import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.team.teamLoop.dto.req.TeamLoopCreateRequest;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopImportance;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdTeamLoopService {
    private final TeamLoopService teamLoopService;
    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;

    // 1번 팀에 1번 팀 루프 ("매일 러닝 3km") + 체크리스트 ("아침 루틴")
    // 2번 팀에 2번 팀 루프 ("매일 파쿠르 30분") + 체크리스트 ("오후 루틴")
    @Transactional
    public void createTeamLoops(Long team1Id, Long team2Id) {
        Member user1 = getMemberByEmailOrThrow("user1@example.com");
        Member user2 = getMemberByEmailOrThrow("user2@example.com");
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusWeeks(1);
        List<DayOfWeek> allDays = Arrays.asList(DayOfWeek.values());
        // 1번 팀 루프: user1이 생성
        teamLoopService.createTeamLoop(
                team1Id,
                new TeamLoopCreateRequest(
                        "매일 러닝 3km",
                        null,
                        RepeatType.WEEKLY,
                        null,          // specificDate (NONE일 때만 사용)
                        allDays,       // daysOfWeek
                        today,         // startDate
                        endDate,          // endDate
                        List.of("아침 루틴"),
                        TeamLoopType.COMMON,
                        TeamLoopImportance.MEDIUM,
                        null           // targetMemberIds (INDIVIDUAL일 때만 의미)
                ),
                memberConverter.toCurrentUserDto(user1)
        );
        // 2번 팀 루프: user2가 생성
        teamLoopService.createTeamLoop(
                team2Id,
                new TeamLoopCreateRequest(
                        "매일 파쿠르 30분",
                        null,
                        RepeatType.WEEKLY,
                        null,
                        allDays,
                        today,
                        endDate,
                        List.of("오후 루틴"),
                        TeamLoopType.COMMON,
                        TeamLoopImportance.MEDIUM,
                        null
                ),
                memberConverter.toCurrentUserDto(user2)
        );
        log.info("[NOT_PROD] TeamLoops created. team1Id={}, team2Id={}", team1Id, team2Id);
    }

    private Member getMemberByEmailOrThrow(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
    }
}
