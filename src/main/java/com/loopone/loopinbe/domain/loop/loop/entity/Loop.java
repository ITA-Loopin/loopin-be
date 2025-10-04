package com.loopone.loopinbe.domain.loop.loop.entity;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private LocalDate loopDate;

    @Column(length = 500)
    private String content; //루프 설명 (메모 또는 부가정보)

    @OneToMany(mappedBy = "loop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoopChecklist> loopChecklists = new ArrayList<>();

    //연관관계 편의 메서드
    public void addChecklist(LoopChecklist checklist) {
        this.loopChecklists.add(checklist);
        checklist.setLoop(this);
    }
}
