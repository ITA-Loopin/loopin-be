package com.loopone.loopinbe.domain.chat.chatRoom.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRoomType {
    ALL("전체"),
    AI("ai 채팅"),
    TEAM("팀 채팅");

    private final String description;
}
