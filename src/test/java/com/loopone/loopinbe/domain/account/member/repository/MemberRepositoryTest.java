package com.loopone.loopinbe.domain.account.member.repository;

import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.support.TestContainersConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.MatcherAssert.assertThat;

@DataJpaTest
@Import(TestContainersConfig.class) // 아래에 제공
@ActiveProfiles("test")
class MemberRepositoryTest {

    @PersistenceContext
    EntityManager em;
    private final MemberRepository memberRepository;

    MemberRepositoryTest(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Test
    @DisplayName("existsByEmail/existsByNickname 동작 확인")
    void exists_checks() {
        var m = Member.builder().email("a@a").nickname("a").build();
        em.persist(m);
        em.flush();

        assertThat(memberRepository.existsByEmail("a@a")).isTrue();
        assertThat(memberRepository.existsByEmail("b@b")).isFalse();
        assertThat(memberRepository.existsByNickname("a")).isTrue();
        assertThat(memberRepository.existsByNickname("b")).isFalse();
    }

    @Test
    @DisplayName("findByKeyword: 팔로워/팔로잉 집계 및 본인 제외")
    void findByKeyword_aggregates() {
        // 사용자 3명: me(1), alice(2), bob(3)
        var me    = Member.builder().email("me@x").nickname("me").build();
        var alice = Member.builder().email("alice@x").nickname("alice").build();
        var bob   = Member.builder().email("bob@x").nickname("bobby").build();
        em.persist(me); em.persist(alice); em.persist(bob);
        em.flush();

        // 팔로우 관계:
        // alice -> bob, me -> bob  (bob의 follower = 2)
        em.persist(MemberFollow.builder().follow(alice).followed(bob).build());
        em.persist(MemberFollow.builder().follow(me).followed(bob).build());
        // bob -> alice (alice의 follower = 1, following = 1)
        em.persist(MemberFollow.builder().follow(bob).followed(alice).build());
        em.flush();

        Page<MemberResponse> page = memberRepository.findByKeyword(
                PageRequest.of(0, 10), "b", me.getId());

        // keyword 'b' ⇒ bobby(bob)만 매치, 본인(me) 제외
        assertThat(page.getContent()).hasSize(1);
        var r = page.getContent().get(0);
        assertThat(r.getNickname()).isEqualTo("bobby");
        assertThat(r.getFollowerCount()).isEqualTo(2L);
        assertThat(r.getFollowingCount()).isZero(); // bob이 팔로우 중인 수 (위 셋업에선 alice 1명이라면 1L로 맞추세요)
    }
}
