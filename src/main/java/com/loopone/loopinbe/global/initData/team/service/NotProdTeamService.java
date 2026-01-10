package com.loopone.loopinbe.global.initData.team.service;

import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.team.team.dto.req.TeamCreateRequest;
import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import com.loopone.loopinbe.domain.team.team.service.TeamService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdTeamService {
    private final TeamService teamService;
    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;

    // 1번 유저가 1번 팀 생성 + 2번 유저 초대
    // 2번 유저가 2번 팀 생성 + 1번 유저 초대
    @Transactional
    public SeedTeamsResult createTeams() {
        Member user1 = getMemberByEmailOrThrow("user1@example.com");
        Member user2 = getMemberByEmailOrThrow("user2@example.com");
        Member user3 = getMemberByEmailOrThrow("user3@example.com");
        // 에펙 마스터 팀: user1(리더) -> user2 초대
        Long team1Id = teamService.createTeam(
                new TeamCreateRequest(
                        TeamCategory.ROUTINE,
                        "에펙 마스터",
                        "3개월 동안 에펙 초보 탈출하기",
                        List.of(user2.getNickname()) // invitedNicknames
                ),
                memberConverter.toCurrentUserDto(user1)
        );
        // 스프링 정복하기 팀: user2(리더) -> user1 초대
        Long team2Id = teamService.createTeam(
                new TeamCreateRequest(
                        TeamCategory.ROUTINE,
                        "스프링 정복하기",
                        "3개월 동안 스프링 개발해보기",
                        List.of(user1.getNickname())
                ),
                memberConverter.toCurrentUserDto(user2)
        );
        // 정처기 도전 팀: user3(리더) -> user1 초대
        Long team3Id = teamService.createTeam(
                new TeamCreateRequest(
                        TeamCategory.ROUTINE,
                        "정처기 도전",
                        "정처기 합격하기",
                        List.of(user2.getNickname())
                ),
                memberConverter.toCurrentUserDto(user3)
        );
        log.info("[NOT_PROD] Teams created. team1Id={}, team2Id={}, team3Id={}", team1Id, team2Id, team3Id);
        return new SeedTeamsResult(team1Id, team2Id, team3Id);
    }

    private Member getMemberByEmailOrThrow(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
    }

    public record SeedTeamsResult(Long team1Id, Long team2Id, Long team3Id) {}
}
