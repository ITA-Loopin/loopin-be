package com.loopone.loopinbe.domain.team.team.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.team.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    // 특정 멤버 객체로 가입된 팀 목록 조회 (조인으로 팀 정보까지 조회)
    List<TeamMember> findAllByMember(Member member);

    // 특정 멤버 ID로 가입된 팀 목록 조회 (ID만으로 빠르게 조회)
    List<TeamMember> findAllByMemberId(Long memberId);

    // 해당 사용자가 팀 멤버인지 확인
    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);
}
