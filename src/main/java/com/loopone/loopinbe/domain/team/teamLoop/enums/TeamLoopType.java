package com.loopone.loopinbe.domain.team.teamLoop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamLoopType {
    COMMON("공통"),  // 팀원 전체
    INDIVIDUAL("개인"); // 지정한 일부 팀원

    private final String description;
}