package com.loopone.loopinbe.domain.team.team.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TeamMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    //사용자별 팀 정렬 순서 (null이면 createdAt 기준, 값이 있으면 해당 순서 우선)
    @Column(name = "sort_order")
    private Integer sortOrder;
}