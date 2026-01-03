package com.loopone.loopinbe.domain.team.team.dto.req;

import com.loopone.loopinbe.domain.team.team.enums.TeamCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "팀 생성을 위한 요청 DTO")
public record TeamCreateRequest(
        @Schema(description = "루프 타입 (PROJECT, CONTEST, STUDY, ROUTINE, CLUB, EXTERNALACTIVITY, ETC)")
        @NotNull(message = "루프 타입은 필수입니다.")
        TeamCategory category,

        @Schema(description = "팀 루프 이름")
        @NotBlank(message = "루프 이름은 필수입니다.")
        String name,

        @Schema(description = "팀 루프 목표")
        @NotBlank(message = "루프 목표는 필수입니다.")
        String goal,

        @Schema(description = "초대할 팀원 닉네임 리스트")
        List<String> invitedNicknames
) {}