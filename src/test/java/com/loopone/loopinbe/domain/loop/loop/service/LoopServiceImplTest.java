package com.loopone.loopinbe.domain.loop.loop.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.loop.loop.mapper.LoopMapper;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRuleRepository;
import com.loopone.loopinbe.domain.loop.loop.serviceImpl.LoopServiceImpl;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

//LoopServiceImpl 단위 테스트
@ExtendWith(MockitoExtension.class)
class LoopServiceImplTest {

    //테스트 대상
    @InjectMocks
    private LoopServiceImpl loopService;

    //Mock 객체
    @Mock
    private LoopRepository loopRepository;

    @Mock
    private LoopRuleRepository loopRuleRepository;

    @Mock
    private LoopMapper loopMapper;

    @Mock
    private MemberConverter memberConverter;

    //테스트용 공통 데이터
    private CurrentUserDto testUser;
    private Member testMember;

    @BeforeEach
    void setUp() {
        //테스트에 사용할 유저 DTO
        testUser = new CurrentUserDto(
                100L, "user1@example.com", null, "testUser",
                null, null, null, null,
                Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.NONE, null
        );

        //유저 DTO가 변환될 Member 엔티티
        testMember = Member.builder()
                .id(100L)
                .email("user1@example.com")
                .nickname("testUser")
                .build();
    }

    // ========== createLoop(루프 생성) 테스트 ==========
    @Test
    @DisplayName("루프 생성 - 단일(NONE) 루프 생성 시 LoopRule 없이 Loop만 저장")
    void createLoop_Type_NONE_ShouldSaveLoopOnceWithoutRule() {
        //given
        LocalDate specificDate = LocalDate.now().plusDays(1);
        LoopCreateRequest request = new LoopCreateRequest(
                "단일 루프 테스트", "내용", RepeatType.NONE,
                specificDate, null, null, null,
                List.of("체크리스트1", "체크리스트2")
        );
        ArgumentCaptor<Loop> loopCaptor = ArgumentCaptor.forClass(Loop.class);
        //어떤 CurrentUserDto가 들어오든 testMember로 변환
        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);

        //when
        loopService.createLoop(request, testUser);

        //then
        //LoopRule은 저장되면 안 됨
        verify(loopRuleRepository, times(0)).save(any(LoopRule.class));
        //Loop는 1번만 저장되어야 함
        verify(loopRepository, times(1)).save(loopCaptor.capture()); //캡처
        //캡처된 Loop 객체의 값 검증
        Loop savedLoop = loopCaptor.getValue();
        assertThat(savedLoop.getTitle()).isEqualTo("단일 루프 테스트");
        assertThat(savedLoop.getMember().getId()).isEqualTo(100L);
        assertThat(savedLoop.getLoopDate()).isEqualTo(specificDate);
        assertThat(savedLoop.getLoopRule()).isNull(); //LoopRule이 null이어야 함
        assertThat(savedLoop.getLoopChecklists()).hasSize(2);
        assertThat(savedLoop.getLoopChecklists().get(0).getContent()).isEqualTo("체크리스트1");
    }

    @Test
    @DisplayName("루프 생성 - 매주 반복(WEEKLY) 루프 생성 시 LoopRule 1회, Loop 4회 저장")
    void createLoop_Type_WEEKLY_ShouldSaveRuleAndLoops() {
        //given
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 14);
        //월(3, 10일), 수(5, 12일) -> 총 4회 생성되어야 함
        List<DayOfWeek> days = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        LoopCreateRequest request = new LoopCreateRequest(
                "주간 반복 루프", null, RepeatType.WEEKLY,
                null, days, start, end, List.of("주간 체크리스트")
        );
        //캡처 도구 준비
        ArgumentCaptor<LoopRule> ruleCaptor = ArgumentCaptor.forClass(LoopRule.class);
        ArgumentCaptor<List<Loop>> loopsCaptor = ArgumentCaptor.forClass(List.class);

        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
        //loopRuleRepository.save()가 호출되면, 인자 그대로 반환
        given(loopRuleRepository.save(any(LoopRule.class))).willAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        //when
        loopService.createLoop(request, testUser);

        //then
        //LoopRule이 1회 저장되었는지 검증
        verify(loopRuleRepository, times(1)).save(ruleCaptor.capture());
        LoopRule savedRule = ruleCaptor.getValue();
        assertThat(savedRule.getMember()).isEqualTo(testMember);
        assertThat(savedRule.getScheduleType()).isEqualTo(RepeatType.WEEKLY);
        assertThat(savedRule.getDaysOfWeek()).isEqualTo(days);
        assertThat(savedRule.getStartDate()).isEqualTo(start);
        assertThat(savedRule.getEndDate()).isEqualTo(end);

        //Loop 목록이 saveAll로 1회 저장되었는지 검증
        verify(loopRepository, times(1)).saveAll(loopsCaptor.capture());

        //캡처된 Loop 리스트 검증
        List<Loop> savedLoops = loopsCaptor.getValue();
        assertThat(savedLoops).hasSize(4);

        //생성된 날짜 확인 (3, 5, 10, 12일)
        assertThat(savedLoops.get(0).getLoopDate()).isEqualTo(LocalDate.of(2025, 11, 3));
        assertThat(savedLoops.get(1).getLoopDate()).isEqualTo(LocalDate.of(2025, 11, 5));
        assertThat(savedLoops.get(2).getLoopDate()).isEqualTo(LocalDate.of(2025, 11, 10));
        assertThat(savedLoops.get(3).getLoopDate()).isEqualTo(LocalDate.of(2025, 11, 12));

        //모든 Loop가 동일한 LoopRule 객체 인스턴스를 참조하는지 확인
        assertThat(savedLoops.stream().allMatch(loop -> loop.getLoopRule() == savedRule)).isTrue();
    }

    // ========== getDetailLoop(루프 상세 조회) 테스트 ==========
    @Test
    @DisplayName("루프 상세 조회 - 성공")
    void getDetailLoop_Success_ShouldReturnDto() {
        //given
        Long loopId = 1L;
        Loop foundLoop = Loop.builder()
                .id(loopId)
                .title("테스트 루프")
                .member(testMember)
                .loopChecklists(new ArrayList<>())
                .loopRule(null) //단일 루프
                .build();

        LoopDetailResponse mockResponse = LoopDetailResponse.builder()
                .id(loopId)
                .title("테스트 루프")
                .progress(0.0)
                .checklists(List.of())
                .loopRule(null)
                .build();

        //loopRepository.findById(1L)이 호출되면, foundLoop를 반환
        given(loopRepository.findById(loopId)).willReturn(Optional.of(foundLoop));
        //loopMapper.toDetailResponse(foundLoop)가 호출되면, mockResponse를 반환
        given(loopMapper.toDetailResponse(foundLoop)).willReturn(mockResponse);

        //when
        //testUser가 조회를 시도
        LoopDetailResponse response = loopService.getDetailLoop(loopId, testUser);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(loopId);
        assertThat(response.title()).isEqualTo("테스트 루프");
        
        verify(loopRepository).findById(loopId);
        verify(loopMapper).toDetailResponse(foundLoop);
    }

    @Test
    @DisplayName("루프 상세 조회 - 실패 (루프 없음)")
    void getDetailLoop_Fail_LoopNotFound() {
        //given
        Long loopId = 999L;
        //findById(999L)가 호출되면, 빈 Optional을 반환하도록 설정
        given(loopRepository.findById(loopId)).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> loopService.getDetailLoop(loopId, testUser))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode") 
                .isEqualTo(ReturnCode.LOOP_NOT_FOUND); //LOOP_NOT_FOUND 발생 검증
    }

    @Test
    @DisplayName("루프 상세 조회 - 실패 (권한 없음)")
    void getDetailLoop_Fail_NotAuthorized() {
        //given
        Long loopId = 2L;
        //다른 사용자(999L) 생성
        Member otherMember = Member.builder().id(999L).nickname("other").build();
        Loop othersLoop = Loop.builder()
                .id(loopId)
                .title("남의 루프")
                .member(otherMember)
                .build();
        
        given(loopRepository.findById(loopId)).willReturn(Optional.of(othersLoop));

        //when & then
        //testUser(100L)가 조회 시도
        assertThatThrownBy(() -> loopService.getDetailLoop(loopId, testUser))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode")
                .isEqualTo(ReturnCode.NOT_AUTHORIZED); //NOT_AUTHORIZED 발생 검증
    }

    // ========== getDailyLoops(날짜별 루프 조회) 테스트 ==========
    @Test
    @DisplayName("날짜별 루프 조회 - 성공")
    void getDailyLoops_Success() {
        //given
        LocalDate date = LocalDate.now();
        List<Loop> loops = List.of(
                Loop.builder().id(1L).member(testMember).title("Loop1").build(),
                Loop.builder().id(2L).member(testMember).title("Loop2").build()
        );
        DailyLoopsResponse mockResponse = DailyLoopsResponse.builder().totalProgress(50.0).loops(List.of()).build();

        given(loopRepository.findByMemberIdAndLoopDate(testUser.id(), date)).willReturn(loops);
        given(loopMapper.toDailyLoopsResponse(loops)).willReturn(mockResponse);

        //when
        DailyLoopsResponse response = loopService.getDailyLoops(date, testUser);

        //then
        assertThat(response).isNotNull();
        assertThat(response.totalProgress()).isEqualTo(50.0);
        verify(loopRepository).findByMemberIdAndLoopDate(testUser.id(), date);
        verify(loopMapper).toDailyLoopsResponse(loops);
    }

//    // --- 4. updateLoop (단일 루프 수정) 테스트 ---
//
//    @Test
//    @DisplayName("단일 루프 수정 - 성공 (LoopRule 연결이 해제되어야 함)")
//    void updateLoop_Success_ShouldDisconnectLoopRule() {
//        // given
//        Long loopId = 1L;
//        LoopRule oldRule = LoopRule.builder().id(10L).build();
//        Loop loop = Loop.builder()
//                .id(loopId)
//                .member(testMember)
//                .title("Old Title")
//                .loopRule(oldRule) // ★ 기존에 그룹에 속해있었음
//                .build();
//
//        LoopUpdateRequest request = new LoopUpdateRequest("New Title", "New Content", LocalDate.now(), List.of("New CL"));
//
//        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
//        given(loopRepository.findById(loopId)).willReturn(Optional.of(loop));
//
//        // when
//        loopService.updateLoop(loopId, request, testUser);
//
//        // then
//        // 1. findById로 루프를 잘 찾아왔는지
//        verify(loopRepository).findById(loopId);
//
//        // 2. 루프 객체의 필드가 잘 변경되었는지
//        assertThat(loop.getTitle()).isEqualTo("New Title");
//        assertThat(loop.getContent()).isEqualTo("New Content");
//        assertThat(loop.getLoopDate()).isEqualTo(LocalDate.now());
//        assertThat(loop.getLoopChecklists()).hasSize(1);
//        assertThat(loop.getLoopChecklists().get(0).getContent()).isEqualTo("New CL");
//
//        // 3. ★ 핵심: LoopRule과의 연결이 해제되었는지
//        assertThat(loop.getLoopRule()).isNull();
//    }
//
//    // --- 5. deleteLoop (단일 루프 삭제) 테스트 ---
//
//    @Test
//    @DisplayName("단일 루프 삭제 - 성공")
//    void deleteLoop_Success() {
//        // given
//        Long loopId = 1L;
//        Loop loop = Loop.builder().id(loopId).member(testMember).build(); // 소유자 100L
//
//        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
//        given(loopRepository.findById(loopId)).willReturn(Optional.of(loop));
//
//        // when
//        loopService.deleteLoop(loopId, testUser); // 요청자 100L
//
//        // then
//        verify(loopRepository).findById(loopId);
//        verify(loopRepository).delete(loop); // 올바른 Loop 객체를 delete에 넘겼는지 검증
//    }
//
//    // --- 6. deleteLoopGroup (루프 그룹 삭제) 테스트 ---
//
//    @Test
//    @DisplayName("루프 그룹 삭제 - 성공 (미래 루프 삭제, 과거 루프 연결 해제)")
//    void deleteLoopGroup_Success_ShouldDeleteFutureAndDisconnectPast() {
//        // given
//        Long loopRuleId = 1L;
//        LocalDate today = LocalDate.now();
//        LoopRule rule = LoopRule.builder().id(loopRuleId).member(testMember).build(); // 소유자 100L
//
//        // 1. 삭제 대상인 미래 루프
//        Loop futureLoop = Loop.builder().id(10L).loopDate(today.plusDays(1)).loopRule(rule).build();
//        List<Loop> futureLoops = List.of(futureLoop);
//
//        // 2. 연결 해제 대상인 과거 루프
//        Loop pastLoop = Loop.builder().id(11L).loopDate(today.minusDays(1)).loopRule(rule).build();
//        List<Loop> pastLoops = List.of(pastLoop);
//
//        // Mock 설정
//        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
//        given(loopRuleRepository.findById(loopRuleId)).willReturn(Optional.of(rule));
//        given(loopRepository.findAllByLoopRuleAndLoopDateAfter(rule, today)).willReturn(futureLoops); // 미래 루프
//        given(loopRepository.findAllByLoopRuleAndLoopDateBefore(rule, today)).willReturn(pastLoops); // 과거 루프
//
//        // when
//        loopService.deleteLoopGroup(loopRuleId, testUser); // 요청자 100L
//
//        // then
//        // 1. 규칙 조회 및 권한 확인
//        verify(loopRuleRepository).findById(loopRuleId);
//
//        // 2. 미래 루프 삭제
//        verify(loopRepository).findAllByLoopRuleAndLoopDateAfter(rule, today);
//        verify(loopRepository).deleteAll(futureLoops); // ★ 미래 루프 리스트가 삭제됨
//
//        // 3. 과거 루프 연결 해제
//        verify(loopRepository).findAllByLoopRuleAndLoopDateBefore(rule, today);
//        // (JPA) pastLoop 객체는 영속성 컨텍스트에서 변경(loopRule=null)되었으므로,
//        // 별도 save 호출 없이 트랜잭션 커밋 시 UPDATE 쿼리가 나감.
//        // 우리는 객체 자체가 변경되었는지(연결이 끊겼는지) 검증
//        assertThat(pastLoop.getLoopRule()).isNull();
//
//        // 4. 규칙 자체 삭제
//        verify(loopRuleRepository).delete(rule);
//    }
//
//    @Test
//    @DisplayName("루프 그룹 삭제 - 실패 (규칙 없음)")
//    void deleteLoopGroup_Fail_RuleNotFound() {
//        // given
//        Long loopRuleId = 999L;
//        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
//        given(loopRuleRepository.findById(loopRuleId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> loopService.deleteLoopGroup(loopRuleId, testUser))
//                .isInstanceOf(ServiceException.class)
//                .extracting("returnCode")
//                .isEqualTo(ReturnCode.LOOP_RULE_NOT_FOUND);
//    }
//
//    @Test
//    @DisplayName("루프 그룹 삭제 - 실패 (권한 없음)")
//    void deleteLoopGroup_Fail_NotAuthorized() {
//        // given
//        Long loopRuleId = 2L;
//        Member otherMember = Member.builder().id(999L).build();
//        LoopRule othersRule = LoopRule.builder().id(loopRuleId).member(otherMember).build(); // 소유자 999L
//
//        given(memberConverter.toMember(any(CurrentUserDto.class))).willReturn(testMember);
//        given(loopRuleRepository.findById(loopRuleId)).willReturn(Optional.of(othersRule));
//
//        // when & then
//        // 요청자 100L
//        assertThatThrownBy(() -> loopService.deleteLoopGroup(loopRuleId, testUser))
//                .isInstanceOf(ServiceException.class)
//                .extracting("returnCode")
//                .isEqualTo(ReturnCode.NOT_AUTHORIZED);
//    }
}