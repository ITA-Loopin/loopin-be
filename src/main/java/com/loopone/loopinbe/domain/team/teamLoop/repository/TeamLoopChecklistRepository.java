package com.loopone.loopinbe.domain.team.teamLoop.repository;

import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoopChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamLoopChecklistRepository extends JpaRepository<TeamLoopChecklist, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamLoopChecklist c where c.teamLoop.team.id in :teamIds")
    int deleteByTeamIds(@Param("teamIds") List<Long> teamIds);
}
