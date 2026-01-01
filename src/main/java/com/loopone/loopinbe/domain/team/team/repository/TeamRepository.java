package com.loopone.loopinbe.domain.team.team.repository;

import com.loopone.loopinbe.domain.team.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
}
