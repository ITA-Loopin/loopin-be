package com.loopone.loopinbe.domain.loop.loopChecklist.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.domain.loop.loopChecklist.repository.LoopChecklistRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.serviceImpl.LoopChecklistServiceImpl;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TestContainersConfig.class, LoopChecklistServiceImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoopChecklistServiceTest {
    // ===== Real Repositories =====
    @Autowired
    LoopChecklistRepository loopChecklistRepository;
    @Autowired
    LoopRepository loopRepository;
    @Autowired
    MemberRepository memberRepository;

    // ===== SUT =====
    @Autowired
    LoopChecklistServiceImpl loopChecklistService;

    @AfterEach
    void cleanup() {
        loopChecklistRepository.deleteAll();
        loopRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // ===== Helpers =====
    private Member persistMember(String email, String nickname) {
        Member m = Member.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(null)
                .state(Member.State.NORMAL)
                .role(Member.MemberRole.ROLE_USER)
                .oAuthProvider(Member.OAuthProvider.GOOGLE)
                .providerId("pid")
                .build();
        return memberRepository.saveAndFlush(m);
    }

    private Loop persistLoop(Member member, LocalDate loopDate, String title, boolean completed) {
        Loop loop = Loop.builder()
                .member(member)
                .loopDate(loopDate)
                .title(title)
                .completed(completed)
                .build();
        return loopRepository.saveAndFlush(loop);
    }

    private LoopChecklist persistChecklist(Loop loop, String content, boolean completed) {
        LoopChecklist c = LoopChecklist.builder()
                .loop(loop)
                .content(content)
                .completed(completed)
                .build();
        return loopChecklistRepository.saveAndFlush(c);
    }

    private CurrentUserDto cu(Member m) {
        return new CurrentUserDto(
                m.getId(),
                m.getEmail(),
                null,
                m.getNickname(),
                null,
                null,
                null,
                m.getProfileImageUrl(),
                m.getState(),
                m.getRole(),
                m.getOAuthProvider(),
                m.getProviderId());
    }

    // =========================================================
    // addLoopChecklist
    // =========================================================
    @Nested
    @DisplayName("체크리스트 생성")
    class AddLoopChecklist {

        @Test
        @DisplayName("성공: 체크리스트 생성")
        void success() {
            // given
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "테스트 루프", false);
            var request = new LoopChecklistCreateRequest("새 체크리스트");
            var currentUser = cu(member);

            // when
            LoopChecklistResponse response = loopChecklistService.addLoopChecklist(loop.getId(), request, currentUser);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("새 체크리스트");
            assertThat(response.completed()).isFalse();

            var saved = loopChecklistRepository.findById(response.id()).orElseThrow();
            assertThat(saved.getLoop().getId()).isEqualTo(loop.getId());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 루프")
        void loopNotFound() {
            var member = persistMember("user@test.com", "user");
            var request = new LoopChecklistCreateRequest("체크리스트");
            var currentUser = cu(member);

            assertThatThrownBy(() -> loopChecklistService.addLoopChecklist(999L, request, currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.LOOP_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 루프에 체크리스트 추가")
        void accessDenied() {
            var owner = persistMember("owner@test.com", "owner");
            var other = persistMember("other@test.com", "other");
            var loop = persistLoop(owner, LocalDate.now(), "소유자 루프", false);
            var request = new LoopChecklistCreateRequest("체크리스트");
            var otherUser = cu(other);

            assertThatThrownBy(() -> loopChecklistService.addLoopChecklist(loop.getId(), request, otherUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.LOOP_ACCESS_DENIED);
        }
    }

    // =========================================================
    // updateLoopChecklist
    // =========================================================
    @Nested
    @DisplayName("체크리스트 수정")
    class UpdateLoopChecklist {

        @Test
        @DisplayName("성공: 내용 수정")
        void updateContent() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "원래 내용", false);
            var request = new LoopChecklistUpdateRequest("수정된 내용", null);
            var currentUser = cu(member);

            loopChecklistService.updateLoopChecklist(checklist.getId(), request, currentUser);

            var updated = loopChecklistRepository.findById(checklist.getId()).orElseThrow();
            assertThat(updated.getContent()).isEqualTo("수정된 내용");
            assertThat(updated.getCompleted()).isFalse();
        }

        @Test
        @DisplayName("성공: 완료 상태 수정")
        void updateCompleted() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "체크리스트", false);
            var request = new LoopChecklistUpdateRequest(null, true);
            var currentUser = cu(member);

            loopChecklistService.updateLoopChecklist(checklist.getId(), request, currentUser);

            var updated = loopChecklistRepository.findById(checklist.getId()).orElseThrow();
            assertThat(updated.getContent()).isEqualTo("체크리스트");
            assertThat(updated.getCompleted()).isTrue();
        }

        @Test
        @DisplayName("성공: 내용과 완료 상태 모두 수정")
        void updateBoth() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "원래 내용", false);
            var request = new LoopChecklistUpdateRequest("수정된 내용", true);
            var currentUser = cu(member);

            loopChecklistService.updateLoopChecklist(checklist.getId(), request, currentUser);

            var updated = loopChecklistRepository.findById(checklist.getId()).orElseThrow();
            assertThat(updated.getContent()).isEqualTo("수정된 내용");
            assertThat(updated.getCompleted()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 체크리스트")
        void checklistNotFound() {
            var member = persistMember("user@test.com", "user");
            var request = new LoopChecklistUpdateRequest("내용", true);
            var currentUser = cu(member);

            assertThatThrownBy(() -> loopChecklistService.updateLoopChecklist(999L, request, currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.CHECK_LIST_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 체크리스트 수정")
        void accessDenied() {
            var owner = persistMember("owner@test.com", "owner");
            var other = persistMember("other@test.com", "other");
            var loop = persistLoop(owner, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "체크리스트", false);
            var request = new LoopChecklistUpdateRequest("수정 시도", true);
            var otherUser = cu(other);

            assertThatThrownBy(() -> loopChecklistService.updateLoopChecklist(checklist.getId(), request, otherUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.CHECKLIST_ACCESS_DENIED);
        }
    }

    // =========================================================
    // deleteLoopChecklist
    // =========================================================
    @Nested
    @DisplayName("체크리스트 삭제")
    class DeleteLoopChecklist {

        @Test
        @DisplayName("성공: 체크리스트 삭제")
        void success() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "체크리스트", false);
            var currentUser = cu(member);

            loopChecklistService.deleteLoopChecklist(checklist.getId(), currentUser);

            assertThat(loopChecklistRepository.findById(checklist.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 체크리스트")
        void checklistNotFound() {
            var member = persistMember("user@test.com", "user");
            var currentUser = cu(member);

            assertThatThrownBy(() -> loopChecklistService.deleteLoopChecklist(999L, currentUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.CHECK_LIST_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 체크리스트 삭제")
        void accessDenied() {
            var owner = persistMember("owner@test.com", "owner");
            var other = persistMember("other@test.com", "other");
            var loop = persistLoop(owner, LocalDate.now(), "루프", false);
            var checklist = persistChecklist(loop, "체크리스트", false);
            var otherUser = cu(other);

            assertThatThrownBy(() -> loopChecklistService.deleteLoopChecklist(checklist.getId(), otherUser))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.CHECKLIST_ACCESS_DENIED);
        }
    }

    // =========================================================
    // deleteAllChecklist
    // =========================================================
    @Nested
    @DisplayName("체크리스트 전체 삭제")
    class DeleteAllChecklist {

        @Test
        @DisplayName("성공: 루프의 모든 체크리스트 삭제")
        void success() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);
            persistChecklist(loop, "체크리스트 1", false);
            persistChecklist(loop, "체크리스트 2", true);
            persistChecklist(loop, "체크리스트 3", false);

            assertThat(loopChecklistRepository.findByLoopId(loop.getId())).hasSize(3);

            loopChecklistService.deleteAllChecklist(loop.getId());

            assertThat(loopChecklistRepository.findByLoopId(loop.getId())).isEmpty();
        }

        @Test
        @DisplayName("성공: 체크리스트가 없는 루프")
        void emptyChecklist() {
            var member = persistMember("user@test.com", "user");
            var loop = persistLoop(member, LocalDate.now(), "루프", false);

            loopChecklistService.deleteAllChecklist(loop.getId());

            assertThat(loopChecklistRepository.findByLoopId(loop.getId())).isEmpty();
        }
    }
}
