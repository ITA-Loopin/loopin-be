package com.loopone.loopinbe.domain.chat.chatRoom.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class ChatRoomPage {
    // 기본 page, size
    private int page = 0;
    private int size = 15;
    @Getter
    private static final int maxPageSize = 15;
}
