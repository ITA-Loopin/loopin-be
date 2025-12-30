package com.loopone.loopinbe.domain.team.teamLoop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamLoopImportance {
    HIGH("높음"),
    MEDIUM("보통"),
    LOW("낮음");

    private final String description;
}