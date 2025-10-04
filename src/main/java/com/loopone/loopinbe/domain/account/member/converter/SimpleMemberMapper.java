package com.loopone.loopinbe.domain.account.member.converter;

import com.loopone.loopinbe.domain.account.member.dto.res.SimpleMemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import com.loopone.loopinbe.domain.chat.chatRoom.entity.ChatRoomMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SimpleMemberMapper {
    // ---------- ChatRoomMember -> SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "member.id")
    @Mapping(target = "userNickname", source = "member.nickname")
    @Mapping(target = "profileImageUrl", source = "member.profileImageUrl")
    SimpleMemberResponse toSimpleMemberResponse(ChatRoomMember member);

    // ---------- MemberFollow.followed → SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "followed.id")
    @Mapping(target = "userNickname", source = "followed.nickname")
    @Mapping(target = "profileImageUrl", source = "followed.profileImageUrl")
    SimpleMemberResponse toFollowingMember(MemberFollow mf);

    // ---------- MemberFollow.follow → SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "follow.id")
    @Mapping(target = "userNickname", source = "follow.nickname")
    @Mapping(target = "profileImageUrl", source = "follow.profileImageUrl")
    SimpleMemberResponse toFollowerMember(MemberFollow mf);

    // ---------- MemberFollowReq.followRec → SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "followRec.id")
    @Mapping(target = "userNickname", source = "followRec.nickname")
    @Mapping(target = "profileImageUrl", source = "followRec.profileImageUrl")
    SimpleMemberResponse toSimpleMemberFromFollowRec(MemberFollowReq req);

    // ---------- MemberFollowReq.followReq → SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "followReq.id")
    @Mapping(target = "userNickname", source = "followReq.nickname")
    @Mapping(target = "profileImageUrl", source = "followReq.profileImageUrl")
    SimpleMemberResponse toSimpleMemberFromFollowReq(MemberFollowReq req);
}
