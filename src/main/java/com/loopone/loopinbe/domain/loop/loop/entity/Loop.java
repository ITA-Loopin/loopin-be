package com.loopone.loopinbe.domain.loop.loop.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Loop extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title; //루프 제목

    @Column(nullable = false)
    private LocalDate loopDate; //루프 날짜

    @Column(length = 500)
    private String content; //루프 설명 (메모 또는 부가정보)

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @OneToMany(mappedBy = "loop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoopChecklist> loopChecklists = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_rule_id", nullable = true)
    private LoopRule loopRule; //단일 루프일 때는 null, 반복 루프일 때만 생성

    //연관관계 편의 메서드
    public void addChecklist(LoopChecklist checklist) {
        this.loopChecklists.add(checklist);
        checklist.setLoop(this);
    }
}
