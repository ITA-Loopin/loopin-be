package com.loopone.loopinbe.domain.account.member.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import com.loopone.loopinbe.domain.account.member.entity.MemberPage;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowReqRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.serviceImpl.MemberServiceImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
import com.loopone.loopinbe.global.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @org.mockito.Mock MemberRepository memberRepository;
    @org.mockito.Mock MemberFollowReqRepository memberFollowReqRepository;
    @org.mockito.Mock MemberFollowRepository memberFollowRepository;
    @org.mockito.Mock S3Service s3Service;
    @org.mockito.Mock MemberConverter memberConverter;
    @org.mockito.Mock
    ChatRoomService chatRoomService;
    @org.mockito.Mock NotificationEventPublisher notificationEventPublisher;
    @org.mockito.InjectMocks MemberServiceImpl memberService;

    // ====== 헬퍼 ======
    private CurrentUserDto cu(Long id, String email, String nickname, String profileUrl) {
        return new CurrentUserDto(
                id,
                email,
                null,               // password
                nickname,
                null,               // phone
                null,               // gender
                null,               // birthday
                profileUrl,
                Member.State.NORMAL,
                Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.GOOGLE,
                "pid"
        );
    }

    // ====== regularSignUp ======
    @Test
    @DisplayName("회원가입: 이메일/닉네임 중복 없으면 저장하고 AI 채팅방 생성")
    void regularSignUp_success() {
        var req = new MemberCreateRequest(
                "jun@loop.in",
                "jun",
                Member.OAuthProvider.GOOGLE,
                "pid"
        );
        given(memberRepository.existsByEmail("jun@loop.in")).willReturn(false);
        given(memberRepository.existsByNickname("jun")).willReturn(false);
        given(chatRoomService.createAiChatRoom(any(ChatRoomRequest.class), any(Member.class)))
                .willReturn(ChatRoomResponse.builder().id(777L).build());
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        var saved = memberService.regularSignUp(req);

        assertThat(saved.getEmail()).isEqualTo("jun@loop.in");
        assertThat(saved.getChatRoomId()).isEqualTo(777L);
        verify(memberRepository).save(any(Member.class));
        verify(chatRoomService).createAiChatRoom(any(ChatRoomRequest.class), any(Member.class));
    }

    @Test
    @DisplayName("회원가입: 이메일 중복 → ServiceException(EMAIL_ALREADY_USED)")
    void regularSignUp_emailDuplicate() {
        var req = new MemberCreateRequest(
                "dup@loop.in", "nick",
                Member.OAuthProvider.KAKAO, "pid"
        );
        given(memberRepository.existsByEmail("dup@loop.in")).willReturn(true);

        assertThatThrownBy(() -> memberService.regularSignUp(req))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.EMAIL_ALREADY_USED);
    }

    @Test
    @DisplayName("회원가입: 닉네임 중복 → ServiceException(NICKNAME_ALREADY_USED)")
    void regularSignUp_nicknameDuplicate() {
        var req = new MemberCreateRequest(
                "u@loop.in", "dupNick",
                Member.OAuthProvider.KAKAO, "pid"
        );
        given(memberRepository.existsByEmail("u@loop.in")).willReturn(false);
        given(memberRepository.existsByNickname("dupNick")).willReturn(true);

        assertThatThrownBy(() -> memberService.regularSignUp(req))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.NICKNAME_ALREADY_USED);
    }

    // ====== getMyInfo / getMyDetailInfo ======
    @Test
    @DisplayName("내 정보 조회: 성공")
    void getMyInfo_success() {
        var me = cu(1L, "jun@loop.in", "jun", "https://img");
        var member = Member.builder().id(1L).email("jun@loop.in").nickname("jun").build();
        var resp = MemberResponse.builder()
                .id(1L)
                .nickname("jun")
                .profileImageUrl("https://img")
                .followMemberCount(0L)
                .followedMemberCount(0L)
                .chatRoomId(null)
                .build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberConverter.toMemberResponse(member)).willReturn(resp);

        var result = memberService.getMyInfo(me);
        assertThat(result).isEqualTo(resp);
    }

    @Test
    @DisplayName("내 정보 조회: 존재 X → USER_NOT_FOUND")
    void getMyInfo_notFound() {
        var me = cu(100L, "x@x", "nope", null);
        given(memberRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyInfo(me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 상세정보 조회: 성공")
    void getMyDetailInfo_success() {
        var me = cu(1L, "jun@loop.in", "jun", null);
        var member = Member.builder().id(1L).email("jun@loop.in").build();
        var detail = DetailMemberResponse.builder().id(1L).email("jun@loop.in").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberConverter.toDetailMemberResponse(member)).willReturn(detail);

        var result = memberService.getMyDetailInfo(me);
        assertThat(result).isEqualTo(detail);
    }

    @Test
    @DisplayName("내 상세정보 조회: 존재 X → USER_NOT_FOUND")
    void getMyDetailInfo_notFound() {
        var me = cu(2L, "x@x", "x", null);
        given(memberRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyDetailInfo(me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    // ====== getMemberInfo / getDetailMemberInfo ======
    @Test
    @DisplayName("다른 멤버 정보 조회: 성공")
    void getMemberInfo_success() {
        var member = Member.builder().id(10L).email("a@a").nickname("a").build();
        var resp = MemberResponse.builder()
                .id(10L).nickname("a")
                .profileImageUrl(null)
                .followMemberCount(0L)
                .followedMemberCount(0L)
                .chatRoomId(null)
                .build();
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(memberConverter.toMemberResponse(member)).willReturn(resp);

        assertThat(memberService.getMemberInfo(10L)).isEqualTo(resp);
    }

    @Test
    @DisplayName("다른 멤버 정보 조회: 존재 X → USER_NOT_FOUND")
    void getMemberInfo_notFound() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.getMemberInfo(99L))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 멤버 상세조회: 성공")
    void getDetailMemberInfo_success() {
        var member = Member.builder().id(10L).email("a@a").build();
        var detail = DetailMemberResponse.builder().id(10L).email("a@a").build();

        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(memberConverter.toDetailMemberResponse(member)).willReturn(detail);

        assertThat(memberService.getDetailMemberInfo(10L)).isEqualTo(detail);
    }

    @Test
    @DisplayName("다른 멤버 상세조회: 존재 X → USER_NOT_FOUND")
    void getDetailMemberInfo_notFound() {
        given(memberRepository.findById(88L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.getDetailMemberInfo(88L))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    // ====== checkNickname ======
    @Test
    @DisplayName("닉네임 중복 확인: 중복이면 예외")
    void checkNickname_duplicate() {
        given(memberRepository.existsByNickname("dup")).willReturn(true);

        assertThatThrownBy(() -> memberService.checkNickname("dup"))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.NICKNAME_ALREADY_USED);
    }

    @Test
    @DisplayName("닉네임 중복 확인: 중복 아님 → 예외 없음")
    void checkNickname_ok() {
        given(memberRepository.existsByNickname("ok")).willReturn(false);
        assertThatCode(() -> memberService.checkNickname("ok")).doesNotThrowAnyException();
    }

    // ====== updateMember ======
    @Test
    @DisplayName("회원정보 수정: 이미지 업로드 성공 & 기존 삭제")
    void updateMember_uploadImage() throws Exception {
        var me = cu(1L, "jun@loop.in", "jun", "https://old");
        var member = Member.builder().id(1L).email("jun@loop.in").nickname("jun").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("newNick")).willReturn(false);
        given(s3Service.uploadImageFile(any(MultipartFile.class), eq("profile-image")))
                .willReturn("https://new");

        var req = new MemberUpdateRequest("jun@loop.in", "newNick");
        var file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);

        memberService.updateMember(req, file, me);
        verify(s3Service).deleteFile("https://old");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://new");
    }

    @Test
    @DisplayName("회원정보 수정: 이미지 파일 null → 기존 이미지 있으면 삭제하고 null로 세팅")
    void updateMember_removeImageWhenNull() {
        var me = cu(1L, "jun@loop.in", "jun", "https://old");
        var member = Member.builder()
                .id(1L).email("jun@loop.in").nickname("jun").profileImageUrl("https://old")
                .build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("jun")).willReturn(false);

        var req = new MemberUpdateRequest("jun@loop.in", "jun");
        memberService.updateMember(req, null, me);
        verify(s3Service).deleteFile("https://old");
        assertThat(member.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("회원정보 수정: 업로드 중 IOException → INTERNAL_ERROR")
    void updateMember_uploadIOException() throws Exception {
        var me = cu(1L, "jun@loop.in", "jun", "");
        var member = Member.builder().id(1L).email("jun@loop.in").nickname("jun").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("new")).willReturn(false);

        var file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        willThrow(new IOException("boom")).given(s3Service)
                .uploadImageFile(any(MultipartFile.class), eq("profile-image"));

        var req = new MemberUpdateRequest("jun@loop.in", "new");

        assertThatThrownBy(() -> memberService.updateMember(req, file, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("회원정보 수정: 닉네임 중복 → NICKNAME_ALREADY_USED")
    void updateMember_nicknameDuplicate() {
        var me = cu(1L, "jun@loop.in", "jun", null);
        var member = Member.builder().id(1L).email("jun@loop.in").nickname("jun").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("dup")).willReturn(true);

        var req = new MemberUpdateRequest("jun@loop.in", "dup");

        assertThatThrownBy(() -> memberService.updateMember(req, null, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.NICKNAME_ALREADY_USED);
    }

    // ====== deleteMember ======
    @Test
    @DisplayName("회원탈퇴: 성공(채팅방 탈퇴 후 삭제)")
    void deleteMember_success() {
        var me = cu(3L, "x@x", "x", null);
        var member = Member.builder().id(3L).email("x@x").build();

        given(memberRepository.findById(3L)).willReturn(Optional.of(member));

        memberService.deleteMember(me, "accessToken");
        verify(chatRoomService).leaveAllChatRooms(3L);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("회원탈퇴: 존재 X → USER_NOT_FOUND")
    void deleteMember_notFound() {
        var me = cu(3L, "x@x", "x", null);
        given(memberRepository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(me, "accessToken"))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    // ====== searchMemberInfo ======
    @Test
    @DisplayName("회원 검색: 페이지 사이즈 초과 → PAGE_REQUEST_FAIL")
    void searchMemberInfo_pageTooLarge() {
        try (MockedStatic<MemberPage> mocked = mockStatic(MemberPage.class)) {
            mocked.when(MemberPage::getMaxPageSize).thenReturn(50); // 최대 50
            Pageable pageable = PageRequest.of(0, 100); // 100으로 요청

            assertThatThrownBy(() -> memberService.searchMemberInfo(pageable, "kw", cu(1L, "e", "n", null)))
                    .isInstanceOf(ServiceException.class)
                    .extracting("returnCode").isEqualTo(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    @Test
    @DisplayName("회원 검색: 성공(PageResponse 변환)")
    void searchMemberInfo_success() {
        try (MockedStatic<MemberPage> mocked = mockStatic(MemberPage.class)) {
            mocked.when(MemberPage::getMaxPageSize).thenReturn(50);
            Pageable pageable = PageRequest.of(0, 10);
            var current = cu(1L, "me@me", "me", null);

            var list = List.of(MemberResponse.builder()
                    .id(2L)
                    .nickname("a")
                    .profileImageUrl(null)
                    .followMemberCount(0L)
                    .followedMemberCount(0L)
                    .chatRoomId(null)
                    .build());
            var page = new PageImpl<>(list, pageable, 1);

            given(memberRepository.findByKeyword(pageable, "loop", 1L)).willReturn(page);

            var res = memberService.searchMemberInfo(pageable, "loop", current);
            assertThat(res.getContent().size()).isEqualTo(1);
            assertThat(res.getContent().get(0).getId()).isEqualTo(2L);
            assertThat(res.getPageMeta().getTotalElements()).isEqualTo(1L);
        }
    }

    // ====== followReq / cancelFollowReq / acceptFollowReq / refuseFollowReq ======
    @Test
    @DisplayName("팔로우 요청: 자기 자신 팔로우 → CANNOT_FOLLOW_SELF")
    void followReq_self() {
        var me = cu(10L, "me@me", "me", null);
        assertThatThrownBy(() -> memberService.followReq(10L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.CANNOT_FOLLOW_SELF);
    }

    @Test
    @DisplayName("팔로우 요청: 성공(중복/요청중 아님)")
    void followReq_success() {
        var me = cu(1L, "me@me", "me", "img");
        var meEntity = Member.builder().id(1L).nickname("me").profileImageUrl("img").build();
        var target = Member.builder().id(2L).nickname("you").build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowRepository.existsByFollowAndFollowed(meEntity, target)).willReturn(false);
        given(memberFollowReqRepository.existsByFollowReqAndFollowRec(meEntity, target)).willReturn(false);
        given(memberFollowReqRepository.save(any(MemberFollowReq.class))).willAnswer(inv -> {
            var saved = inv.getArgument(0, MemberFollowReq.class);
            return MemberFollowReq.builder()
                    .id(100L)
                    .followReq(saved.getFollowReq())
                    .followRec(saved.getFollowRec())
                    .build();
        });

        memberService.followReq(2L, me);
        verify(memberFollowReqRepository).save(any(MemberFollowReq.class));
        verify(notificationEventPublisher).publishFollowRequest(any(Notification.class));
    }

    @Test
    @DisplayName("팔로우 요청: 이미 팔로우 중 → ALREADY_FOLLOW")
    void followReq_alreadyFollow() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowRepository.existsByFollowAndFollowed(meEntity, target)).willReturn(true);

        assertThatThrownBy(() -> memberService.followReq(2L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.ALREADY_FOLLOW);
    }

    @Test
    @DisplayName("팔로우 요청: 이미 요청 중 → ALREADY_REQUESTED")
    void followReq_alreadyRequested() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowRepository.existsByFollowAndFollowed(meEntity, target)).willReturn(false);
        given(memberFollowReqRepository.existsByFollowReqAndFollowRec(meEntity, target)).willReturn(true);

        assertThatThrownBy(() -> memberService.followReq(2L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("팔로우 요청 취소: 성공")
    void cancelFollowReq_success() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();
        var req = MemberFollowReq.builder().id(10L).followReq(meEntity).followRec(target).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(meEntity, target)).willReturn(Optional.of(req));

        memberService.cancelFollowReq(2L, me);
        verify(memberFollowReqRepository).delete(req);
    }

    @Test
    @DisplayName("팔로우 요청 취소: 해당 요청 없음 → REQUEST_NOT_FOUND")
    void cancelFollowReq_notFound() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(meEntity, target)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.cancelFollowReq(2L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("팔로우 요청 수락: 성공(요청 삭제 → 팔로우 저장 → 알림 발행)")
    void acceptFollowReq_success() {
        var me = cu(2L, "you@you", "you", "img2");
        var requester = Member.builder().id(1L).nickname("me").build();
        var receiver = Member.builder().id(2L).nickname("you").build();
        var followReq = MemberFollowReq.builder().id(10L).followReq(requester).followRec(receiver).build();
        var savedFollow = MemberFollow.builder().id(99L).follow(requester).followed(receiver).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
        given(memberConverter.toMember(me)).willReturn(receiver);
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)).willReturn(Optional.of(followReq));
        given(memberFollowRepository.save(any(MemberFollow.class))).willReturn(savedFollow);

        memberService.acceptFollowReq(1L, me);
        verify(memberFollowReqRepository).delete(followReq);
        verify(memberFollowRepository).save(any(MemberFollow.class));
        verify(notificationEventPublisher).publishFollowRequest(any(Notification.class));
    }

    @Test
    @DisplayName("팔로우 요청 수락: 요청 없음 → REQUEST_NOT_FOUND")
    void acceptFollowReq_notFound() {
        var me = cu(2L, "you@you", "you", null);
        var requester = Member.builder().id(1L).build();
        var receiver = Member.builder().id(2L).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
        given(memberConverter.toMember(me)).willReturn(receiver);
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.acceptFollowReq(1L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("팔로우 요청 거절: 성공(요청 삭제)")
    void refuseFollowReq_success() {
        var me = cu(2L, "you@you", "you", null);
        var requester = Member.builder().id(1L).build();
        var receiver = Member.builder().id(2L).build();
        var followReq = MemberFollowReq.builder().id(10L).followReq(requester).followRec(receiver).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
        given(memberConverter.toMember(me)).willReturn(receiver);
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)).willReturn(Optional.of(followReq));

        memberService.refuseFollowReq(1L, me);
        verify(memberFollowReqRepository).delete(followReq);
    }

    @Test
    @DisplayName("팔로우 요청 거절: 요청 없음 → REQUEST_NOT_FOUND")
    void refuseFollowReq_notFound() {
        var me = cu(2L, "you@you", "you", null);
        var requester = Member.builder().id(1L).build();
        var receiver = Member.builder().id(2L).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
        given(memberConverter.toMember(me)).willReturn(receiver);
        given(memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.refuseFollowReq(1L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.REQUEST_NOT_FOUND);
    }

    // ====== cancelFollow / removeFollowed ======
    @Test
    @DisplayName("팔로우 취소: 성공")
    void cancelFollow_success() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();
        var relation = MemberFollow.builder().id(5L).follow(meEntity).followed(target).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowRepository.findByFollowAndFollowed(meEntity, target)).willReturn(Optional.of(relation));

        memberService.cancelFollow(2L, me);
        verify(memberFollowRepository).delete(relation);
    }

    @Test
    @DisplayName("팔로우 취소: 관계 없음 → FOLLOW_NOT_FOUND")
    void cancelFollow_notFound() {
        var me = cu(1L, "me@me", "me", null);
        var meEntity = Member.builder().id(1L).build();
        var target = Member.builder().id(2L).build();

        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        given(memberFollowRepository.findByFollowAndFollowed(meEntity, target)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.cancelFollow(2L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.FOLLOW_NOT_FOUND);
    }

    @Test
    @DisplayName("팔로워 목록에서 삭제: 성공")
    void removeFollowed_success() {
        var me = cu(2L, "me@me", "me", null); // me가 followed
        var follower = Member.builder().id(1L).build();                     // follower가 follow
        var meEntity = Member.builder().id(2L).build();
        var relation = MemberFollow.builder().id(7L).follow(follower).followed(meEntity).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(follower));
        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberFollowRepository.findByFollowAndFollowed(follower, meEntity)).willReturn(Optional.of(relation));

        memberService.removeFollowed(1L, me);
        verify(memberFollowRepository).delete(relation);
    }

    @Test
    @DisplayName("팔로워 목록에서 삭제: 관계 없음 → FOLLOWER_NOT_FOUND")
    void removeFollowed_notFound() {
        var me = cu(2L, "me@me", "me", null);
        var follower = Member.builder().id(1L).build();
        var meEntity = Member.builder().id(2L).build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(follower));
        given(memberConverter.toMember(me)).willReturn(meEntity);
        given(memberFollowRepository.findByFollowAndFollowed(follower, meEntity)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.removeFollowed(1L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.FOLLOWER_NOT_FOUND);
    }
}
