package com.loopone.loopinbe.domain.account.member.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleMemberDto {
    private Long userId;
    private String userName;
    private String userNickname;
    private String profileImageUrl;
}
