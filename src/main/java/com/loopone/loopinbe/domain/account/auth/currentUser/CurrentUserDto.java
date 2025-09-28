package com.loopone.loopinbe.domain.account.auth.currentUser;

import com.letzgo.LetzgoBe.domain.account.member.entity.Member;
import com.letzgo.LetzgoBe.domain.account.member.entity.MemberFollow;
import com.letzgo.LetzgoBe.domain.account.member.entity.MemberFollowReq;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CurrentUserDto {
    private Long id;
    private String name;
    private String nickname;
    private String phone;
    private String email;
    private String password;
    private Member.Gender gender;
    private Member.State state;
    private Member.MemberRole role;
    private String profileImageUrl;
    private List<MemberFollow> followList;
    private List<MemberFollow> followedList;
    private List<MemberFollowReq> followReqList;
    private List<MemberFollowReq> followRecList;
    private LocalDate birthday;
    private LocalDateTime createdAt;
}
