package com.loopone.loopinbe.domain.loop.checkList.repository;

import com.loopone.loopinbe.domain.loop.checkList.entity.LoopCheckList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoopCheckListRepository extends JpaRepository<LoopCheckList, Long> {
    // loopId 들을 IN으로 받아 한 번에 가져오고 정렬
    @Query("""
        SELECT s
        FROM LoopCheckList s
        WHERE s.loop.id IN :mainIds
        ORDER BY s.loop.id ASC, s.deadline ASC NULLS LAST, s.createdAt ASC
    """)
    List<LoopCheckList> findByLoopIdInWithOrder(@Param("mainIds") List<Long> mainIds);


    List<LoopCheckList> findByLoopId(Long loopId);
}
