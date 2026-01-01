package com.loopone.loopinbe.domain.team.teamLoop.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
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
public class TeamLoopMemberProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_loop_id")
    private TeamLoop teamLoop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "memberProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopMemberCheck> checks = new ArrayList<>(); //해당 멤버의 체크리스트 상태 저장

    //내 진행률 계산
    public double calculateProgress(int totalChecklistCount) {
        if (totalChecklistCount == 0) return 0.0;

        long checkedCount = this.checks.stream()
                .filter(TeamLoopMemberCheck::isChecked)
                .count();

        return (double) checkedCount / totalChecklistCount * 100.0;
    }
}