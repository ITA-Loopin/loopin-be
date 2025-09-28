package com.loopone.loopinbe.domain.account.member.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String name;
    private String nickname;
    private String profileImageUrl;
    private Long followMemberCount;
    private Long followedMemberCount;

    public MemberResponse(Long id, String name, String nickname,
                          String profileImageUrl,
                          Long followMemberCount,
                          Long followedMemberCount) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.followMemberCount = followMemberCount;
        this.followedMemberCount = followedMemberCount;
    }
}
