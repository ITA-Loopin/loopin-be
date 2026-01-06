package com.loopone.loopinbe.domain.loop.loop.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoopRuleRepository extends JpaRepository<LoopRule, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from LoopRule lr
        where lr.member.id = :memberId
          and not exists (
              select 1
              from TeamLoop tl
              where tl.loopRule = lr
          )
          and not exists (
              select 1
              from Loop l
              where l.loopRule = lr
          )
    """)
    void deletePersonalRulesNotUsedAnywhere(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update LoopRule lr
           set lr.member = :newOwner
         where lr.member.id = :oldOwnerId
           and lr.id in (
               select distinct tl.loopRule.id
                 from TeamLoop tl
                where tl.team.id = :teamId
                  and tl.loopRule is not null
           )
    """)
    int transferOwnerByTeamId(@Param("teamId") Long teamId,
                              @Param("oldOwnerId") Long oldOwnerId,
                              @Param("newOwner") Member newOwner);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from LoopRule lr
     where lr.id in :loopRuleIds
       and not exists (select 1 from TeamLoop tl where tl.loopRule.id = lr.id)
       and not exists (select 1 from Loop l where l.loopRule.id = lr.id)
""")
    int deleteOrphanByIds(@Param("loopRuleIds") List<Long> loopRuleIds);
}
