package com.loopone.loopinbe.domain.account.member.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private Long followMemberCount;
    private Long followedMemberCount;
    private Long chatRoomId;

    public MemberResponse(Long id, String nickname, String profileImageUrl,
                          Long followMemberCount, Long followedMemberCount, Long chatRoomId) {
        this.id = id;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.followMemberCount = followMemberCount;
        this.followedMemberCount = followedMemberCount;
        this.chatRoomId = chatRoomId;
    }

    // --- 별칭 게터(테스트 호환) ---
    public Long getFollowingCount() { return followMemberCount; }    // 내가 팔로우하는 수
    public Long getFollowerCount()  { return followedMemberCount; }  // 나를 팔로우하는 수
}
