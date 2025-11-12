package com.loopone.loopinbe.domain.account.member.repository;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.support.TestContainersConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class MemberRepositoryTest {
    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    private void flushAndClear() {
        em.flush();
        em.clear();
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
    @DisplayName("findIdByEmail: 이메일로 ID만 조회된다")
    void findIdByEmail_returnsIdOnly() {
        var m = Member.builder().email("idonly@x").nickname("idonly").build();
        em.persist(m);
        flushAndClear();

        var found = memberRepository.findIdByEmail("idonly@x");
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(m.getId());

        assertThat(memberRepository.findIdByEmail("nope@x")).isEmpty();
    }

    @Test
    @DisplayName("findByEmail: 엔티티 그래프 기반으로 멤버를 제대로 로딩한다")
    void findByEmail_entityGraphLoadsMember() {
        var alice = Member.builder().email("alice@x").nickname("alice").build();
        var bob   = Member.builder().email("bob@x").nickname("bobby").build();
        em.persist(alice);
        em.persist(bob);
        em.persist(MemberFollow.builder().follow(alice).followed(bob).build());
        flushAndClear();

        var loaded = memberRepository.findByEmail("bob@x");
        assertThat(loaded).isPresent();
        var m = loaded.get();
        assertThat(m.getId()).isNotNull();
        assertThat(m.getEmail()).isEqualTo("bob@x");
        assertThat(m.getNickname()).isEqualTo("bobby");
    }

    @Test
    @DisplayName("findByKeyword: 팔로워/팔로잉 집계 및 본인 제외")
    void findByKeyword_aggregates() {
        var me    = Member.builder().email("me@x").nickname("me").build();
        var alice = Member.builder().email("alice@x").nickname("alice").build();
        var bob   = Member.builder().email("bob@x").nickname("bobby").build();
        em.persist(me); em.persist(alice); em.persist(bob);
        em.flush();

        // alice -> bob, me -> bob  (bob의 follower = 2)
        em.persist(MemberFollow.builder().follow(alice).followed(bob).build());
        em.persist(MemberFollow.builder().follow(me).followed(bob).build());
        // bob -> alice (alice의 follower = 1, following = 1)
        em.persist(MemberFollow.builder().follow(bob).followed(alice).build());
        em.flush();

        var page = memberRepository.findByKeyword(PageRequest.of(0, 10), "b", me.getId());

        assertThat(page.getContent()).hasSize(1);
        var r = page.getContent().get(0);
        assertThat(r.getNickname()).isEqualTo("bobby");
        assertThat(r.getFollowerCount()).isEqualTo(2L);
        assertThat(r.getFollowingCount()).isEqualTo(1L);
    }
}
