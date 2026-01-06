package com.loopone.loopinbe.domain.team.team.repository;

import com.loopone.loopinbe.domain.team.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query("select t from Team t where t.leader.id = :leaderId")
    List<Team> findAllByLeaderId(@Param("leaderId") Long leaderId);
}
