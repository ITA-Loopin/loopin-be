package com.loopone.loopinbe.domain.loop.loop.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.support.TestContainersConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//LoopRepository 통합 테스트
@DataJpaTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoopRepositoryTest {

    @Autowired
    private LoopRepository loopRepository;

    @PersistenceContext
    private EntityManager em;

    private Member testMember;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder().email("test@loop.in").nickname("testUser").build();
        otherMember = Member.builder().email("other@loop.in").nickname("otherUser").build();

        em.persist(testMember);
        em.persist(otherMember);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    //findByMemberIdAndLoopDate 테스트
    @Test
    @DisplayName("findByMemberIdAndLoopDate - 특정 멤버의 특정 날짜 루프만 조회")
    void findByMemberIdAndLoopDate_Success() {
        //given
        LocalDate today = LocalDate.now();

        //성공 대상 - testMember의 오늘 루프
        Loop loop1 = Loop.builder().member(testMember).title("루프1").loopDate(today).build();
        em.persist(loop1);
        //실패 대상 - testMember의 어제 루프 (날짜 다름)
        Loop loop2 = Loop.builder().member(testMember).title("루프2").loopDate(today.minusDays(1)).build();
        em.persist(loop2);
        //실패 대상 - otherMember의 오늘 루프 (멤버 다름)
        Loop loop3 = Loop.builder().member(otherMember).title("루프3").loopDate(today).build();
        em.persist(loop3);

        flushAndClear();

        //when
        List<Loop> loops = loopRepository.findByMemberIdAndLoopDate(testMember.getId(), today);

        //then
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0).getId()).isEqualTo(loop1.getId()); //루프1만 포함
    }

    //findAllByLoopRuleAndLoopDateAfter 테스트
    @Test
    @DisplayName("findAllByLoopRuleAndLoopDateAfter - 특정 그룹의 오늘 및 미래 루프만 조회")
    void findAllByLoopRuleAndLoopDateAfter_Success() {
        //given
        LocalDate today = LocalDate.now();

        LoopRule rule = LoopRule.builder().member(testMember).scheduleType(RepeatType.WEEKLY).build();
        em.persist(rule);
        //성공 대상 - 오늘
        Loop todayLoop = Loop.builder().member(testMember).title("오늘").loopDate(today).loopRule(rule).build();
        em.persist(todayLoop);
        //성공 대상 - 미래
        Loop futureLoop = Loop.builder().member(testMember).title("미래").loopDate(today.plusDays(1)).loopRule(rule).build();
        em.persist(futureLoop);
        //실패 대상 - 과거
        Loop pastLoop = Loop.builder().member(testMember).title("과거").loopDate(today.minusDays(1)).loopRule(rule).build();
        em.persist(pastLoop);
        //실패 대상 - 그룹 다름
        Loop otherLoop = Loop.builder().member(testMember).title("다른 그룹").loopDate(today).loopRule(null).build();
        em.persist(otherLoop);

        flushAndClear();

        //when
        List<Loop> loops = loopRepository.findAllByLoopRuleAndLoopDateAfter(rule, today);

        //then
        assertThat(loops).hasSize(2);
        //조회된 루프의 ID가 todayLoop와 futureLoop의 ID와 일치하는지 확인
        assertThat(loops).extracting(Loop::getId)
                .containsExactlyInAnyOrder(todayLoop.getId(), futureLoop.getId());
    }

    //findAllByLoopRuleAndLoopDateBefore 테스트
    @Test
    @DisplayName("findAllByLoopRuleAndLoopDateBefore - 특정 그룹의 과거 루프만 조회")
    void findAllByLoopRuleAndLoopDateBefore_Success() {
        //given
        LocalDate today = LocalDate.now();

        LoopRule rule = LoopRule.builder().member(testMember).scheduleType(RepeatType.WEEKLY).build();
        em.persist(rule);

        //성공 대상 - 과거
        Loop pastLoop = Loop.builder().member(testMember).title("과거").loopDate(today.minusDays(1)).loopRule(rule).build();
        em.persist(pastLoop);
        //실패 대상 - 오늘
        Loop todayLoop = Loop.builder().member(testMember).title("오늘").loopDate(today).loopRule(rule).build();
        em.persist(todayLoop);

        flushAndClear();

        //when
        List<Loop> loops = loopRepository.findAllByLoopRuleAndLoopDateBefore(rule, today);

        //then
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0).getId()).isEqualTo(pastLoop.getId());
    }
}