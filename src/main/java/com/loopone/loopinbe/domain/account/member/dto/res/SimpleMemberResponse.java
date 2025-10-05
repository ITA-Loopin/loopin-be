package com.loopone.loopinbe.domain.account.member.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleMemberResponse {
    private Long userId;
    private String userNickname;
    private String profileImageUrl;
}
