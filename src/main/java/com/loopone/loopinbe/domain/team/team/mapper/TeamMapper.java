package com.loopone.loopinbe.domain.team.team.mapper;

import com.loopone.loopinbe.domain.team.team.dto.res.MyTeamResponse;
import com.loopone.loopinbe.domain.team.team.dto.res.RecruitingTeamResponse;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    //TeamMember를 MyTeamResponse로 변환
    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.category", target = "category")
    @Mapping(source = "team.name", target = "name")
    @Mapping(source = "team.goal", target = "goal")
    // TODO: 진행률 계산 로직 추가 필요
    @Mapping(target = "totalProgress", constant = "0")
    MyTeamResponse toMyTeamResponse(TeamMember teamMember);

    //Team을 RecruitingTeamResponse로 변환
    @Mapping(source = "id", target = "teamId")
    @Mapping(target = "currentMemberCount", expression = "java(team.getTeamMembers().size())")
    RecruitingTeamResponse toRecruitingTeamResponse(Team team);
}