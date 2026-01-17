package com.loopone.loopinbe.domain.team.teamLoop.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.team.teamLoop.enums.TeamLoopStatus;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
// 각 멤버에 대한 TeamLoopMemberCheck들을 관리하는 엔티티
public class TeamLoopMemberProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_loop_id")
    private TeamLoop teamLoop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TeamLoopStatus status = TeamLoopStatus.NOT_STARTED;

    @OneToMany(mappedBy = "memberProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopMemberCheck> checks = new ArrayList<>(); // 해당 멤버의 체크리스트 상태 저장

    // 내 진행률 계산
    public double calculateProgress(int totalChecklistCount) {
        // 명시적으로 완료 처리된 경우 100% 반환
        if (this.status == TeamLoopStatus.COMPLETED) {
            return 100.0;
        }

        if (totalChecklistCount == 0)
            return 0.0;

        long checkedCount = this.checks.stream()
                .filter(TeamLoopMemberCheck::isChecked)
                .count();

        return (double) checkedCount / totalChecklistCount * 100.0;
    }
}