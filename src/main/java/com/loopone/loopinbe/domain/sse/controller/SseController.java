package com.loopone.loopinbe.domain.sse.controller;

import com.loopone.loopinbe.domain.sse.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/rest-api/v1/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "SSE API")
public class SseController {

    private final SseEmitterService sseEmitterService;

    @Operation(summary = "SSE 구독", description = "채팅방의 이벤트를 SSE로 구독합니다.")
    @GetMapping(value = "/subscribe/{chatRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long chatRoomId,
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return sseEmitterService.subscribe(chatRoomId, lastEventId);
    }
}
