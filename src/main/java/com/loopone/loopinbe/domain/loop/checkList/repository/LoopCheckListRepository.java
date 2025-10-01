package com.loopone.loopinbe.domain.loop.checkList.repository;

import com.loopone.loopinbe.domain.loop.checkList.entity.LoopCheckList;
import org.checkerframework.checker.units.qual.N;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoopCheckListRepository extends JpaRepository<LoopCheckList, Long> {
     //여러 루프 ID에 해당하는 모든 체크리스트를 IN으로 한 번에 조회
    @Query("""
        SELECT lc
        FROM LoopCheckList lc
        WHERE lc.loop.id IN :loopIds
        ORDER BY lc.loop.id ASC, lc.createdAt ASC
    """)
    List<LoopCheckList> findByLoopIdIn(@Param("loopIds") List<Long> loopIds);

    //특정 루프 ID에 해당하는 모든 체크리스트를 조회
    List<LoopCheckList> findByLoopId(Long loopId);
}
