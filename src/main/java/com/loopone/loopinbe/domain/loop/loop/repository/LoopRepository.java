package com.loopone.loopinbe.domain.loop.loop.repository;

import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoopRepository extends JpaRepository<Loop, Long> {
    // 특정 멤버의 모든 루프 목록을 조회
    @Query("""
        SELECT m
        FROM Loop m
        WHERE m.member.id = :memberId
        ORDER BY m.loopDate DESC, m.createdAt DESC
    """)
    Page<Loop> findByMemberIdWithOrder(@Param("memberId") Long memberId, Pageable pageable);

    //특정 멤버의 특정 날짜에 해당하는 모든 루프 목록을 조회
    List<Loop> findByMemberIdAndLoopDate(Long memberId, LocalDate loopDate);

    //그룹의 루프 전체를 리스트로 조회 (오늘 포함 미래만 조회)
    @Query("""
        SELECT l
        FROM Loop l
        WHERE l.loopGroup = :loopGroup AND l.loopDate >= :date
    """)
    List<Loop> findAllByLoopGroupAndLoopDateAfter(@Param("loopGroup") String loopGroup, @Param("date") LocalDate date);
}
