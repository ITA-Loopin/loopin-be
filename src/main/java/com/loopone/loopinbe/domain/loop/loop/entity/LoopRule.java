package com.loopone.loopinbe.domain.loop.loop.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.checkerframework.checker.signature.qual.Identifier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class LoopRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RepeatType scheduleType;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "loop_rule_days_of_week", joinColumns = @JoinColumn(name = "loop_rule_id"))
    private List<DayOfWeek> daysOfWeek;

    private LocalDate startDate;

    private LocalDate endDate;

}
