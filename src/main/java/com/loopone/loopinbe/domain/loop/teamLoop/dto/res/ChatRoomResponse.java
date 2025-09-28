package com.loopone.loopinbe.domain.loop.teamLoop.dto.res;

import com.letzgo.LetzgoBe.domain.account.member.dto.res.SimpleMemberDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private Long memberId;
    private String title;
    private List<SimpleMemberDto> chatRoomMembers;
    private Long unreadCount;
    private String lastMessage;
    private LocalDateTime lastMessageCreatedAt;
    private int memberCount;
}
