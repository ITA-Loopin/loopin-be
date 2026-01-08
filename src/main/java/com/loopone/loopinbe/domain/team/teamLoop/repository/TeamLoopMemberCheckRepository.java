package com.loopone.loopinbe.domain.team.teamLoop.repository;

import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopMemberCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamLoopMemberCheckRepository extends JpaRepository<TeamLoopMemberCheck, Long> {
    // (B) 내 체크만 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamLoopMemberCheck c " +
            "where c.memberProgress.member.id = :memberId and c.memberProgress.teamLoop.team.id in :teamIds")
    int deleteByMemberAndTeamIds(@Param("memberId") Long memberId,
                                 @Param("teamIds") List<Long> teamIds);

    // (A) 팀 전체 체크 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamLoopMemberCheck c " +
            "where c.memberProgress.teamLoop.team.id in :teamIds")
    int deleteByTeamIds(@Param("teamIds") List<Long> teamIds);

    @Query("""
        SELECT c FROM TeamLoopMemberCheck c
        WHERE c.memberProgress.member.id = :memberId
        AND c.checklist.id = :checklistId
    """)
    Optional<TeamLoopMemberCheck> findByMemberIdAndChecklistId(
            @Param("memberId") Long memberId,
            @Param("checklistId") Long checklistId
    );

    List<TeamLoopMemberCheck> findByMemberProgressIdOrderByIdAsc(Long memberProgressId);
}
