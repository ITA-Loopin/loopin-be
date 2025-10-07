package com.loopone.loopinbe.domain.loop.ai.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIRequestProducer;
import com.loopone.loopinbe.domain.loop.ai.service.LoopAIService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest-api/v1/loopAI")
@Tag(name = "LoopAI", description = "루프 AI API")
public class ApiV1LoopAIController {
//    private final LoopAIService loopAIService;
//    private final LoopAIRequestProducer loopAIRequestProducer;
//
//    @PostMapping("")
//    @Operation(summary = "AI 루프 생성")
//    public ApiResponse<String> chat(
//            @RequestParam String prompt,
//            @CurrentUser CurrentUserDto user
//    ) {
//        String requestId = UUID.randomUUID().toString();
//
//        loopAIRequestProducer.sendRequest(requestId, prompt);
//
//        return ApiResponse.success(requestId);
//    }
//
//    @GetMapping("")
//    @Operation(summary = "AI 불러오기")
//    public ApiResponse<String> getResult(
//            @RequestParam String requestId,
//            @CurrentUser CurrentUserDto user
//    ) {
//        String result = loopAIService.getAIResult(requestId);
//
//        return ApiResponse.success(result);
//    }
}
