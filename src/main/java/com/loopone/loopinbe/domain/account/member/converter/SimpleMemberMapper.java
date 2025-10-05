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

    // MemberFollow.follow -> SimpleMemberResponse (내가 '팔로잉'하는 사람)
    @Named("toFollowingMember")
    @Mapping(target = "userId", source = "follow.id")
    @Mapping(target = "userNickname", source = "follow.nickname")
    @Mapping(target = "profileImageUrl", source = "follow.profileImageUrl")
    SimpleMemberResponse toFollowerMember(MemberFollow mf);

    // MemberFollow.followed -> SimpleMemberResponse (나를 '팔로우'하는 사람)
    @Named("toFollowerMember")
    @Mapping(target = "userId", source = "followed.id")
    @Mapping(target = "userNickname", source = "followed.nickname")
    @Mapping(target = "profileImageUrl", source = "followed.profileImageUrl")
    SimpleMemberResponse toFollowingMember(MemberFollow mf);

    // MemberFollowReq.followReq -> SimpleMemberResponse (내가 팔로우 요청 보낸 대상)
    @Named("toSimpleMemberFromFollowReq")
    @Mapping(target = "userId", source = "followRec.id")
    @Mapping(target = "userNickname", source = "followRec.nickname")
    @Mapping(target = "profileImageUrl", source = "followRec.profileImageUrl")
    SimpleMemberResponse toSimpleMemberFromFollowReq(MemberFollowReq req);

    // MemberFollowReq.followRec -> SimpleMemberResponse (나한테 팔로우 요청한 사람)
    @Named("toSimpleMemberFromFollowRec")
    @Mapping(target = "userId", source = "followReq.id")
    @Mapping(target = "userNickname", source = "followReq.nickname")
    @Mapping(target = "profileImageUrl", source = "followReq.profileImageUrl")
    SimpleMemberResponse toSimpleMemberFromFollowRec(MemberFollowReq req);

    // ---------- List 변환 ----------

    @Named("toFollowingMembers")
    default List<SimpleMemberResponse> toFollowingMembers(List<MemberFollow> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toFollowingMember).toList();
    }

    @Named("toFollowerMembers")
    default List<SimpleMemberResponse> toFollowerMembers(List<MemberFollow> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toFollowerMember).toList();
    }

    @Named("toSimpleMembersFromFollowReqs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowReqs(List<MemberFollowReq> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSimpleMemberFromFollowReq).toList();
    }

    @Named("toSimpleMembersFromFollowRecs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowRecs(List<MemberFollowReq> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSimpleMemberFromFollowRec).toList();
    }
}
