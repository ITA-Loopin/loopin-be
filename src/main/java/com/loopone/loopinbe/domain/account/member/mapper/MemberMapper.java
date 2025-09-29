package com.loopone.loopinbe.domain.account.member.mapper;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.SimpleMemberDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberMapper {
    // ---------- Member -> MemberResponse ----------
    @Mapping(target = "followMemberCount", expression = "java(source.getFollowList() != null ? (long) source.getFollowList().size() : 0L)")
    @Mapping(target = "followedMemberCount", expression = "java(source.getFollowedList() != null ? (long) source.getFollowedList().size() : 0L)")
    MemberResponse toMemberResponse(Member source);

    // ---------- Member -> DetailMemberResponse ----------
    @Mapping(target = "followMemberCount", expression = "java(source.getFollowList() != null ? (long) source.getFollowList().size() : 0L)")
    @Mapping(target = "followedMemberCount", expression = "java(source.getFollowedList() != null ? (long) source.getFollowedList().size() : 0L)")
    @Mapping(target = "followList", source = "followList", qualifiedByName = "toFollowingMembers")
    @Mapping(target = "followedList", source = "followedList", qualifiedByName = "toFollowerMembers")
    @Mapping(target = "followReqList", source = "followReqList", qualifiedByName = "toSimpleMembersFromFollowReqs")
    @Mapping(target = "followRecList", source = "followRecList", qualifiedByName = "toSimpleMembersFromFollowRecs")
    DetailMemberResponse toDetailMemberResponse(Member source);

    // ---------- LoginUserDto <-> Member ----------
    // 관계 필드나 콜렉션은 무시 (지연 로딩 유발/불필요한 merge 방지)
    @Mapping(target = "followList", ignore = true)
    @Mapping(target = "followedList", ignore = true)
    @Mapping(target = "followReqList", ignore = true)
    @Mapping(target = "followRecList", ignore = true)
    Member toMember(CurrentUserDto source);

    // 관계 필드나 콜렉션은 무시 (지연 로딩 유발/불필요한 merge 방지)
    @Mapping(target = "followList", ignore = true)
    @Mapping(target = "followedList", ignore = true)
    @Mapping(target = "followReqList", ignore = true)
    @Mapping(target = "followRecList", ignore = true)
    CurrentUserDto toCurrentUserDto(Member source);

    // ---------- List 단위 매핑(팔로우/요청) ----------
    @Named("toFollowingMembers")
    default List<SimpleMemberDto> toFollowingMembers(List<MemberFollow> followList) {
        if (followList == null) return List.of();
        return followList.stream()
                .map(this::toFollowingMember)
                .collect(Collectors.toList());
    }

    @Named("toFollowerMembers")
    default List<SimpleMemberDto> toFollowerMembers(List<MemberFollow> followedList) {
        if (followedList == null) return List.of();
        return followedList.stream()
                .map(this::toFollowerMember)
                .collect(Collectors.toList());
    }

    @Named("toSimpleMembersFromFollowRecs")
    default List<SimpleMemberDto> toSimpleMembersFromFollowRecs(List<MemberFollowReq> reqs) {
        if (reqs == null) return List.of();
        return reqs.stream()
                .map(this::toSimpleMemberFromFollowRec)
                .collect(Collectors.toList());
    }

    @Named("toSimpleMembersFromFollowReqs")
    default List<SimpleMemberDto> toSimpleMembersFromFollowReqs(List<MemberFollowReq> reqs) {
        if (reqs == null) return List.of();
        return reqs.stream()
                .map(this::toSimpleMemberFromFollowReq)
                .collect(Collectors.toList());
    }

    // ---------- Element 단위 매핑(팔로우/요청) ----------
    // MemberFollow.followed -> SimpleMemberDto
    default SimpleMemberDto toFollowingMember(MemberFollow mf) {
        if (mf == null || mf.getFollowed() == null) return null;
        var m = mf.getFollowed();
        return SimpleMemberDto.builder()
                .userId(m.getId())
                .userName(m.getName())
                .userNickname(m.getNickname())
                .profileImageUrl(m.getProfileImageUrl())
                .build();
    }

    // MemberFollow.follow -> SimpleMemberDto
    default SimpleMemberDto toFollowerMember(MemberFollow mf) {
        if (mf == null || mf.getFollow() == null) return null;
        var m = mf.getFollow();
        return SimpleMemberDto.builder()
                .userId(m.getId())
                .userName(m.getName())
                .userNickname(m.getNickname())
                .profileImageUrl(m.getProfileImageUrl())
                .build();
    }

    // MemberFollowReq.followRec -> SimpleMemberDto
    default SimpleMemberDto toSimpleMemberFromFollowRec(MemberFollowReq req) {
        if (req == null || req.getFollowRec() == null) return null;
        var m = req.getFollowRec();
        return SimpleMemberDto.builder()
                .userId(m.getId())
                .userName(m.getName())
                .userNickname(m.getNickname())
                .profileImageUrl(m.getProfileImageUrl())
                .build();
    }

    // MemberFollowReq.followReq -> SimpleMemberDto
    default SimpleMemberDto toSimpleMemberFromFollowReq(MemberFollowReq req) {
        if (req == null || req.getFollowReq() == null) return null;
        var m = req.getFollowReq();
        return SimpleMemberDto.builder()
                .userId(m.getId())
                .userName(m.getName())
                .userNickname(m.getNickname())
                .profileImageUrl(m.getProfileImageUrl())
                .build();
    }
}
