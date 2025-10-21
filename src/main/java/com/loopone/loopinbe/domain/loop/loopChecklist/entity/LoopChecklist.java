package com.loopone.loopinbe.domain.loop.loopChecklist.entity;

import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class LoopChecklist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_id", nullable = false)
    private Loop loop;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false; //완료 여부 (기본값 false)
}
