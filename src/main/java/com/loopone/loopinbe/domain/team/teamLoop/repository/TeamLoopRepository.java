package com.loopone.loopinbe.domain.team.teamLoop.repository;

import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.teamLoop.entity.TeamLoop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface TeamLoopRepository extends JpaRepository<TeamLoop, Long> {

    List<TeamLoop> findByTeamAndLoopDate(Team team, LocalDate loopDate);

    @Query("""
        SELECT tl
        FROM TeamLoop tl
        WHERE tl.team.id = :teamId AND tl.loopDate = :date
        ORDER BY
            CASE tl.importance
                WHEN 'HIGH' THEN 1
                WHEN 'MIDDLE' THEN 2
                WHEN 'LOW' THEN 3
            END ASC,
            tl.id DESC
    """)
    List<TeamLoop> findAllByTeamIdAndDate(@Param("teamId") Long teamId, @Param("date") LocalDate date);
}
