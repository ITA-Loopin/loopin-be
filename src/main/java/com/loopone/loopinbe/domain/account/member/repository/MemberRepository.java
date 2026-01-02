package com.loopone.loopinbe.domain.account.member.repository;

import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 이메일로 회원 존재여부 확인
    boolean existsByEmail(String email);

    // 고유한 닉네임인지 확인
    boolean existsByNickname(String nickname);

    // 이메일로 id 조회
    @Query("select m.id from Member m where m.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    // 이메일로 회원 조회
    @EntityGraph(value = "Member.withAllRelations", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Member> findByEmail(String email);

    // 회원 검색하기
    @Query(
            value = """
            SELECT new com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse(
                m.id,
                m.email,
                m.nickname,
                m.profileImageUrl
            )
            FROM Member m
            WHERE m.id <> :currentUserId
              AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """,
            countQuery = """
            SELECT COUNT(m.id)
            FROM Member m
            WHERE m.id <> :currentUserId
              AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """
    )
    Page<MemberResponse> findByKeyword(
            Pageable pageable,
            @Param("keyword") String keyword,
            @Param("currentUserId") Long currentUserId
    );

    List<Member> findAllByNicknameIn(List<String> invitedNicknames);
}
