package com.loopone.loopinbe.domain.team.team.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamInvitationCreateRequest {
    @NotNull(message = "초대할 멤버 ID는 필수입니다.")
    private Long inviteeId;
}
