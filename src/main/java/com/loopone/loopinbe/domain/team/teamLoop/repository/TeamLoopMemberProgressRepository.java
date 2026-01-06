package com.loopone.loopinbe.domain.team.teamLoop.repository;

import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamLoopMemberProgressRepository extends JpaRepository<TeamLoopMemberProgress, Long> {
    // (B) 내 progress만 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamLoopMemberProgress p " +
            "where p.member.id = :memberId and p.teamLoop.team.id in :teamIds")
    int deleteByMemberAndTeamIds(@Param("memberId") Long memberId,
                                 @Param("teamIds") List<Long> teamIds);

    // (A) 팀 전체 progress 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamLoopMemberProgress p " +
            "where p.teamLoop.team.id in :teamIds")
    int deleteByTeamIds(@Param("teamIds") List<Long> teamIds);
}
