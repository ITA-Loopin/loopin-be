package com.loopone.loopinbe.domain.loop.loop.repository;

import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MainGoalRepository extends JpaRepository<Loop, Long> {
    // 멤버가 작성한 모든 목표
    @Query("""
        SELECT m
        FROM Loop m
        WHERE m.member.id = :memberId
        ORDER BY m.deadline ASC NULLS LAST, m.createdAt ASC
    """)
    Page<Loop> findByMemberIdWithOrder(@Param("memberId") Long memberId, Pageable pageable);

    // 멤버가 작성한 미완료 목표
    @Query("""
    select mg from Loop mg
    where mg.member.id = :memberId
      and (mg.checked = false or mg.checked is null)
    order by 
      case when mg.deadline is null then 1 else 0 end,
      mg.deadline asc,
      mg.createdAt asc
    """)
    Page<Loop> findForRag(@Param("memberId") Long memberId, Pageable pageable);
}
