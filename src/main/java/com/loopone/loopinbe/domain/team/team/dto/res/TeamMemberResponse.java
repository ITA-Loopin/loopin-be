package com.loopone.loopinbe.domain.team.team.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "팀원 목록 조회를 위한 응답 DTO")
public record TeamMemberResponse(
        @Schema(description = "회원 ID")
        Long memberId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImage
) {}