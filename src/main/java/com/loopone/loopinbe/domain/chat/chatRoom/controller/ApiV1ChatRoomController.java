package com.loopone.loopinbe.domain.chat.chatRoom.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomListResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest-api/v1/chat-room")
@Tag(name = "ChatRoom", description = "채팅방 API")
public class ApiV1ChatRoomController {
    private final ChatRoomService chatRoomService;

    @GetMapping("")
    @Operation(summary = "채팅방 리스트 조회", description = "AI채팅방 리스트를 조회합니다.")
    public ApiResponse<ChatRoomListResponse> getChatRooms(
            @Parameter(hidden = true) @CurrentUser CurrentUserDto user
    ) {
        return ApiResponse.success(chatRoomService.getChatRooms(user.id()));
    }
}
