package com.loopone.loopinbe.domain.team.teamLoop.entity;

import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopImportance;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopType;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TeamLoop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate loopDate;

    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamLoopType type; //루프 유형

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamLoopImportance importance; // 중요도

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_rule_id", nullable = true)
    private LoopRule loopRule;

    @OneToMany(mappedBy = "teamLoop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopMemberProgress> memberProgress = new ArrayList<>();

    @OneToMany(mappedBy = "teamLoop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopChecklist> teamLoopChecklists = new ArrayList<>();

    // 팀 전체 평균 진행률 계산
    public double calculateTeamProgress() {
        if (this.memberProgress.isEmpty()) return 0.0;
        int totalChecklistCount = this.teamLoopChecklists.size();
        if (totalChecklistCount == 0) return 0.0;

        return this.memberProgress.stream()
                .mapToDouble(p -> p.calculateProgress(totalChecklistCount)) // Progress 엔티티 메서드 호출
                .average()
                .orElse(0.0);
    }

    // 특정 멤버의 개인 진행률 계산
    public double calculatePersonalProgress(Long memberId) {
        int totalChecklistCount = this.teamLoopChecklists.size();
        if (totalChecklistCount == 0) return 0.0;

        return this.memberProgress.stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .findFirst()
                .map(p -> p.calculateProgress(totalChecklistCount))
                .orElse(0.0);
    }

    // 참여자인지 확인
    public boolean isParticipating(Long memberId) {
        return this.memberProgress.stream()
                .anyMatch(p -> p.getMember().getId().equals(memberId));
    }
}
