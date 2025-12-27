package com.loopone.loopinbe.domain.team.team.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamState {
    IN_PROGRESS("진행 중"),
    BEFORE_START("시작 전"),
    ENDED("종료됨");

    private final String description;
}
