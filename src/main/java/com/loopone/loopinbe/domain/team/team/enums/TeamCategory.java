package com.loopone.loopinbe.domain.team.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamCategory {
    PROJECT("팀프로젝트"),
    CONTEST("공모전"),
    STUDY("스터디"),
    ROUTINE("루틴 공유"),
    ETC("기타");

    private final String description;
}
