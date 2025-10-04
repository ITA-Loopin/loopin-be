package com.loopone.loopinbe.domain.account.member.converter;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.SimpleMemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = SimpleMemberMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberConverter {
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
    Member toMember(CurrentUserDto source);
    CurrentUserDto toCurrentUserDto(Member source);

    // ---------- List 단위 매핑(팔로우/요청) ----------
    @Named("toFollowingMembers")
    default List<SimpleMemberResponse> toFollowingMembers(List<MemberFollow> list, @Context SimpleMemberMapper mapper) {
        return list == null ? List.of() :
                list.stream().map(mapper::toFollowingMember).collect(Collectors.toList());
    }

    @Named("toFollowerMembers")
    default List<SimpleMemberResponse> toFollowerMembers(List<MemberFollow> list, @Context SimpleMemberMapper mapper) {
        return list == null ? List.of() :
                list.stream().map(mapper::toFollowerMember).collect(Collectors.toList());
    }

    @Named("toSimpleMembersFromFollowReqs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowReqs(List<MemberFollowReq> list, @Context SimpleMemberMapper mapper) {
        return list == null ? List.of() :
                list.stream().map(mapper::toSimpleMemberFromFollowReq).collect(Collectors.toList());
    }

    @Named("toSimpleMembersFromFollowRecs")
    default List<SimpleMemberResponse> toSimpleMembersFromFollowRecs(List<MemberFollowReq> list, @Context SimpleMemberMapper mapper) {
        return list == null ? List.of() :
                list.stream().map(mapper::toSimpleMemberFromFollowRec).collect(Collectors.toList());
    }
}
