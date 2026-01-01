package com.loopone.loopinbe.domain.team.teamLoop.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.team.teamLoop.dto.res.TeamLoopListResponse;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoop;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberCheck;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberProgress;
import com.loopone.loopinbe.domain.team.teamLoop.repository.TeamLoopRepository;
import com.loopone.loopinbe.domain.team.teamLoop.service.TeamLoopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamLoopServiceImpl implements TeamLoopService {
    private final TeamLoopRepository teamLoopRepository;

    @Override
    public List<TeamLoopListResponse> getTeamLoops(Long teamId, CurrentUserDto currentUser) {
        LocalDate today = LocalDate.now();

        List<TeamLoop> teamLoops = teamLoopRepository.findAllByTeamIdAndDate(teamId, today);
        Long myId = currentUser.id();

        return teamLoops.stream()
                .map(loop -> {
                    //참여 여부
                    boolean isParticipating = loop.isParticipating(myId);
                    //해당 루프의 내 진행률
                    double myProgress = isParticipating ?
                            loop.calculatePersonalProgress(myId) : 0.0;
                    //해당 루프의 팀 진행률
                    double teamProgress = loop.calculateTeamProgress();

                    return TeamLoopListResponse.builder()
                            .id(loop.getId())
                            .title(loop.getTitle())
                            .loopDate(loop.getLoopDate())
                            .type(loop.getType())
                            .importance(loop.getImportance())
                            .teamProgress(teamProgress)
                            .personalProgress(myProgress)
                            .isParticipating(isParticipating)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ========== 비즈니스 로직 메서드 ==========
    //TODO: 계산 로직 UTIL 클래스로 TeamServiceImpl와 통일
    private double calculateTeamLoopAverage(TeamLoop loop) {
        List<TeamLoopMemberProgress> allProgresses = loop.getMemberProgress();
        if (allProgresses.isEmpty()) return 0.0;
        int totalChecklistCount = loop.getTeamLoopChecks().size();
        if (totalChecklistCount == 0) return 0.0;

        return allProgresses.stream()
                .mapToDouble(p -> calculateProgressFromChecks(p, totalChecklistCount))
                .average().orElse(0.0);
    }

    private double calculatePersonalProgress(TeamLoop loop, Long memberId) {
        int totalChecklistCount = loop.getTeamLoopChecks().size();
        if (totalChecklistCount == 0) return 0.0;

        return loop.getMemberProgress().stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .findFirst()
                .map(p -> calculateProgressFromChecks(p, totalChecklistCount))
                .orElse(0.0);
    }

    private double calculateProgressFromChecks(TeamLoopMemberProgress progress, int totalCount) {
        long checkedCount = progress.getChecks().stream()
                .filter(TeamLoopMemberCheck::isChecked)
                .count();
        return (double) checkedCount / totalCount * 100.0;
    }

    private boolean isParticipating(TeamLoop loop, Long memberId) {
        return loop.getMemberProgress().stream()
                .anyMatch(p -> p.getMember().getId().equals(memberId));
    }
}
