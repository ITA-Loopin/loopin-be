package com.loopone.loopinbe.domain.account.member.dto.res;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DetailMemberResponse {
    private Long id;
    private String email;
    private String nickname;
//    private String phone;
//    private Member.Gender gender;  // 성별
//    private LocalDate birthday;
    private String profileImageUrl;
    private Long followMemberCount;
    private Long followedMemberCount;
    private List<SimpleMemberResponse> followList;
    private List<SimpleMemberResponse> followedList;
    private List<SimpleMemberResponse> followReqList;
    private List<SimpleMemberResponse> followRecList;
    private Long chatRoomId;
}
