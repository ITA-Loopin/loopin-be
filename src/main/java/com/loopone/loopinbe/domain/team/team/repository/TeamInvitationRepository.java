package com.loopone.loopinbe.domain.team.team.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.team.team.entity.Team;
import com.loopone.loopinbe.domain.team.team.entity.TeamInvitation;
import com.loopone.loopinbe.domain.team.team.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    boolean existsByTeamAndInviteeAndStatus(Team team, Member invitee, InvitationStatus status);

    List<TeamInvitation> findByTeamAndStatus(Team team, InvitationStatus status);

    List<TeamInvitation> findByInviteeAndStatus(Member invitee, InvitationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamInvitation ti where ti.inviter.id = :memberId or ti.invitee.id = :memberId")
    int deleteAllByMemberInvolved(@Param("memberId") Long memberId);
}
