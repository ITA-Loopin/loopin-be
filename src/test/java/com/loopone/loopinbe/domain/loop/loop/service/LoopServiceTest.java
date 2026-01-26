package com.loopone.loopinbe.domain.loop.loop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.mapper.MemberMapperImpl;
import com.loopone.loopinbe.domain.account.member.mapper.SimpleMemberMapperImpl;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.repository.LoopChecklistRepository;
import com.loopone.loopinbe.domain.loop.loop.mapper.LoopMapperImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.loopone.loopinbe.domain.chat.chatMessage.service.ChatMessageService;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCompletionUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopCalendarResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRuleRepository;
import com.loopone.loopinbe.domain.loop.loop.serviceImpl.LoopServiceImpl;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.ai.AiEventPublisher;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TestContainersConfig.class, LoopServiceImpl.class, LoopMapperImpl.class, MemberMapperImpl.class,
        SimpleMemberMapperImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoopServiceTest {
    // ===== Real Repositories =====
    @Autowired
    LoopRepository loopRepository;
    @Autowired
    LoopRuleRepository loopRuleRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LoopChecklistRepository loopChecklistRepository;
    @Autowired
    ChatRoomRepository chatRoomRepository;

    // ===== SUT =====
    @Autowired
    LoopServiceImpl loopService;

    // ===== External boundaries (mock) =====
    @MockitoBean
    ChatMessageService chatMessageService;
    @MockitoBean
    AiEventPublisher aiEventPublisher;
    @MockitoBean
    NotificationEventPublisher notificationEventPublisher;

    @AfterEach
    void cleanup() {
        // 순서 중요(관계 테이블 먼저)
        loopChecklistRepository.deleteAll();
        loopRepository.deleteAll();
        loopRuleRepository.deleteAll();
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

    private Loop persistLoop(Member member, LocalDate date, String title, boolean completed) {
        Loop loop = Loop.builder()
                .member(member)
                .loopDate(date)
                .title(title)
                .completed(completed)
                .loopRule(null)
                .build();
        return loopRepository.saveAndFlush(loop);
    }

    // =========================================================
    // createLoop - 단일 루프
    // =========================================================
    @Nested
    class CreateLoop {

        @Test
        @DisplayName("성공: 단일 루프 생성(반복 없음)")
        void createSingleLoop_success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            var req = new LoopCreateRequest(
                    "단일 루프",
                    "설명",
                    RepeatType.NONE,
                    LocalDate.of(2026, 1, 20),
                    null,
                    null,
                    null,
                    List.of("체크리스트1"),
                    null);

            Long loopId = loopService.createLoop(req, dto);

            assertThat(loopId).isNotNull();
            var saved = loopRepository.findById(loopId).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("단일 루프");
            assertThat(saved.getLoopDate()).isEqualTo(LocalDate.of(2026, 1, 20));
            assertThat(saved.getMember().getId()).isEqualTo(member.getId());
            assertThat(saved.getLoopRule()).isNull();
        }

        @Test
        @DisplayName("성공: 매주 반복 루프 생성 (여러 개 생성)")
        void createWeeklyLoop_success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            var req = new LoopCreateRequest(
                    "매주 루프",
                    null,
                    RepeatType.WEEKLY,
                    null,
                    List.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 1, 20),
                    LocalDate.of(2026, 2, 28),
                    null,
                    null);

            Long loopId = loopService.createLoop(req, dto);

            assertThat(loopId).isNotNull();
            var allLoops = loopRepository.findAll();
            assertThat(allLoops).hasSizeGreaterThan(1); // 여러 개 생성됨
            assertThat(allLoops).allMatch(loop -> loop.getLoopRule() != null);
        }

        @Test
        @DisplayName("성공: 매월 반복 루프 생성")
        void createMonthlyLoop_success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            var req = new LoopCreateRequest(
                    "매월 루프",
                    null,
                    RepeatType.MONTHLY,
                    null,
                    null,
                    LocalDate.of(2026, 1, 15),
                    LocalDate.of(2026, 6, 30),
                    null,
                    null);

            Long loopId = loopService.createLoop(req, dto);

            assertThat(loopId).isNotNull();
            var allLoops = loopRepository.findAll();
            assertThat(allLoops).hasSizeGreaterThanOrEqualTo(5); // 6개월 치
        }
    }

    // =========================================================
    // getDetailLoop
    // =========================================================
    @Nested
    class GetDetailLoop {

        @Test
        @DisplayName("성공: 루프 상세 조회")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var loop = persistLoop(member, LocalDate.now(), "테스트 루프", false);

            LoopDetailResponse result = loopService.getDetailLoop(loop.getId(), dto);

            assertThat(result.id()).isEqualTo(loop.getId());
            assertThat(result.title()).isEqualTo("테스트 루프");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 루프 -> LOOP_NOT_FOUND")
        void notFound() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            assertThatThrownBy(() -> loopService.getDetailLoop(999L, dto))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.LOOP_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 루프 조회 -> NOT_AUTHORIZED")
        void unauthorizedAccess() {
            var owner = persistMember("owner@loop.in", "owner");
            var other = persistMember("other@loop.in", "other");
            var loop = persistLoop(owner, LocalDate.now(), "소유자 루프", false);

            var otherDto = cu(other);

            assertThatThrownBy(() -> loopService.getDetailLoop(loop.getId(), otherDto))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // =========================================================
    // getDailyLoops
    // =========================================================
    @Nested
    class GetDailyLoops {

        @Test
        @DisplayName("성공: 특정 날짜의 루프 목록 조회")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var targetDate = LocalDate.of(2026, 1, 20);

            persistLoop(member, targetDate, "루프1", false);
            persistLoop(member, targetDate, "루프2", false);
            persistLoop(member, targetDate.plusDays(1), "다른날루프", false);

            DailyLoopsResponse result = loopService.getDailyLoops(targetDate, dto);

            assertThat(result.loops()).hasSize(2);
            assertThat(result.loops()).extracting("title")
                    .containsExactlyInAnyOrder("루프1", "루프2");
        }

        @Test
        @DisplayName("성공: 루프가 없는 날짜 조회 시 빈 목록 반환")
        void emptyList() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var targetDate = LocalDate.of(2026, 1, 20);

            DailyLoopsResponse result = loopService.getDailyLoops(targetDate, dto);

            assertThat(result.loops()).isEmpty();
        }
    }

    // =========================================================
    // updateLoopCompletion
    // =========================================================
    @Nested
    class UpdateLoopCompletion {

        @Test
        @DisplayName("성공: 루프 완료 처리")
        void complete() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var loop = persistLoop(member, LocalDate.now(), "테스트 루프", false);

            var req = new LoopCompletionUpdateRequest(true);

            loopService.updateLoopCompletion(loop.getId(), req, dto);

            var updated = loopRepository.findById(loop.getId()).orElseThrow();
            assertThat(updated.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("성공: 루프 미완료 처리")
        void incomplete() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var loop = persistLoop(member, LocalDate.now(), "테스트 루프", true);

            var req = new LoopCompletionUpdateRequest(false);

            loopService.updateLoopCompletion(loop.getId(), req, dto);

            var updated = loopRepository.findById(loop.getId()).orElseThrow();
            assertThat(updated.isCompleted()).isFalse();
        }
    }

    // =========================================================
    // updateLoop (단일 루프 수정)
    // =========================================================
    @Nested
    class UpdateLoop {

        @Test
        @DisplayName("성공: 단일 루프 수정 (그룹에서 제외)")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var loop = persistLoop(member, LocalDate.now(), "원본루프", false);

            var req = new LoopUpdateRequest(
                    "수정된루프",
                    "수정된 내용",
                    LocalDate.now().plusDays(1),
                    null);

            loopService.updateLoop(loop.getId(), req, dto);

            var updated = loopRepository.findById(loop.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("수정된루프");
            assertThat(updated.getLoopDate()).isEqualTo(LocalDate.now().plusDays(1));
            assertThat(updated.getLoopRule()).isNull(); // 그룹에서 제외됨
        }
    }

    // =========================================================
    // deleteLoop (단일 루프 삭제)
    // =========================================================
    @Nested
    class DeleteLoop {

        @Test
        @DisplayName("성공: 단일 루프 삭제")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);
            var loop = persistLoop(member, LocalDate.now(), "삭제할루프", false);

            loopService.deleteLoop(loop.getId(), dto);

            assertThat(loopRepository.findById(loop.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 루프 삭제 -> LOOP_NOT_FOUND")
        void notFound() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            assertThatThrownBy(() -> loopService.deleteLoop(999L, dto))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.LOOP_NOT_FOUND);
        }
    }

    // =========================================================
    // deleteLoopGroup (루프 그룹 삭제)
    // =========================================================
    @Nested
    class DeleteLoopGroup {

        @Test
        @DisplayName("성공: 루프 그룹 전체 삭제")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            // LoopRule 생성
            var loopRule = LoopRule.builder()
                    .member(member)
                    .scheduleType(RepeatType.WEEKLY)
                    .build();
            loopRule = loopRuleRepository.saveAndFlush(loopRule);

            // 여러 루프 생성
            var loop1 = persistLoop(member, LocalDate.now(), "그룹루프1", false);
            loop1.setLoopRule(loopRule);
            var loop2 = persistLoop(member, LocalDate.now().plusDays(7), "그룹루프2", false);
            loop2.setLoopRule(loopRule);
            loopRepository.flush();

            loopService.deleteLoopGroup(loop1.getId(), dto);

            // 해당 날짜 이후 루프들이 삭제되었는지 확인
            assertThat(loopRepository.findById(loop1.getId())).isEmpty();
            assertThat(loopRepository.findById(loop2.getId())).isEmpty();
        }
    }

    // =========================================================
    // deleteMyLoops (사용자의 모든 루프 삭제)
    // =========================================================
    @Nested
    class DeleteMyLoops {

        @Test
        @DisplayName("성공: 사용자의 모든 루프 삭제")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            persistLoop(member, LocalDate.now(), "루프1", false);
            persistLoop(member, LocalDate.now().plusDays(1), "루프2", false);

            loopService.deleteMyLoops(member.getId());

            assertThat(loopRepository.findAll()).isEmpty();
        }
    }

    // =========================================================
    // getLoopCalendar
    // =========================================================
    @Nested
    class GetLoopCalendar {

        @Test
        @DisplayName("성공: 루프 캘린더 조회")
        void success() {
            var member = persistMember("test@loop.in", "tester");
            var dto = cu(member);

            persistLoop(member, LocalDate.of(2026, 1, 15), "루프1", false);
            persistLoop(member, LocalDate.of(2026, 1, 20), "루프2", false);
            persistLoop(member, LocalDate.of(2026, 2, 10), "루프3", true);

            LoopCalendarResponse result = loopService.getLoopCalendar(2026, 1, dto);

            assertThat(result).isNotNull();
        }
    }
}
