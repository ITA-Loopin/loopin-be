package com.loopone.loopinbe.domain.loop.loopChecklist.repository;

import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoopChecklistRepository extends JpaRepository<LoopChecklist, Long> {
     //여러 루프 ID에 해당하는 모든 체크리스트를 IN으로 한 번에 조회
    @Query("""
        SELECT lc
        FROM LoopChecklist lc
        WHERE lc.loop.id IN :loopIds
        ORDER BY lc.loop.id ASC, lc.createdAt ASC
    """)
    List<LoopChecklist> findByLoopIdIn(@Param("loopIds") List<Long> loopIds);

    //특정 루프 ID에 해당하는 모든 체크리스트를 조회
    List<LoopChecklist> findByLoopId(Long loopId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
delete from LoopChecklist c
where c.loop.id in (
    select l.id from Loop l
    where l.loopRule.id = :ruleId
      and l.loopDate >= :targetDate
)
""")
    int deleteFutureChecklists(@Param("ruleId") Long ruleId, @Param("targetDate") LocalDate targetDate);
}
