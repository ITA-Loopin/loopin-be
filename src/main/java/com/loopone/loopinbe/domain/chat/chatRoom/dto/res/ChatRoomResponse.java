package com.loopone.loopinbe.domain.chat.chatRoom.dto.res;

import com.loopone.loopinbe.domain.account.member.dto.res.SimpleMemberResponse;
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
    private List<SimpleMemberResponse> chatRoomMembers;
}
