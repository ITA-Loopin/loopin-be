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

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SimpleMemberMapper {
    // ---------- ChatRoomMember -> SimpleMemberResponse ----------
    @Mapping(target = "userId", source = "member.id")
    @Mapping(target = "userNickname", source = "member.nickname")
    @Mapping(target = "profileImageUrl", source = "member.profileImageUrl")
    SimpleMemberResponse toSimpleMemberResponse(ChatRoomMember member);

    @Named("toFollowingMember")
    SimpleMemberResponse toFollowingMember(MemberFollow mf); // mf.getFollowed() -> SimpleMemberResponse

    @Named("toFollowerMember")
    SimpleMemberResponse toFollowerMember(MemberFollow mf);  // mf.getFollow() -> SimpleMemberResponse

    @Named("toSimpleMemberFromFollowReq")
    SimpleMemberResponse toSimpleMemberFromFollowReq(MemberFollowReq req);

    @Named("toSimpleMemberFromFollowRec")
    SimpleMemberResponse toSimpleMemberFromFollowRec(MemberFollowReq req);

    // ---------- MemberFollow.follow → SimpleMemberResponse ----------
    @Named("toFollowingMembers")
    default List<SimpleMemberResponse> toFollowingMembers(List<MemberFollow> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toFollowingMember).toList();
    }

    // ---------- MemberFollow.followed → SimpleMemberResponse ----------
    @Named("toFollowerMembers")
    default List<SimpleMemberResponse> toFollowerMembers(List<MemberFollow> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toFollowerMember).toList();
    }

    // ---------- MemberFollowReq.followReq → SimpleMemberResponse ----------
    @Named("toSimpleMembersFromFollowReqs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowReqs(List<MemberFollowReq> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSimpleMemberFromFollowReq).toList();
    }

    // ---------- MemberFollowReq.followRec → SimpleMemberResponse ----------
    @Named("toSimpleMembersFromFollowRecs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowRecs(List<MemberFollowReq> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSimpleMemberFromFollowRec).toList();
    }
}
