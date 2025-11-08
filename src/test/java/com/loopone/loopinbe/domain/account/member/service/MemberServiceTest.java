package com.loopone.loopinbe.domain.account.member.service;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowReqRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.serviceImpl.MemberServiceImpl;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.req.ChatRoomRequest;
import com.loopone.loopinbe.domain.chat.chatRoom.dto.res.ChatRoomResponse;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
import com.loopone.loopinbe.global.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    MemberFollowReqRepository memberFollowReqRepository;
    @Mock
    MemberFollowRepository memberFollowRepository;
    @Mock
    S3Service s3Service;
    @Mock
    MemberConverter memberConverter;
    @Mock
    ChatRoomService chatRoomService;
    @Mock
    NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    MemberServiceImpl memberService;

    @Test
    @DisplayName("회원가입: 이메일/닉네임 중복 없으면 저장하고 AI 채팅방 생성")
    void regularSignUp_success() {
        var req = new MemberCreateRequest("jun@loop.in", "jun", null, null, "GOOGLE", "pid");
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
        var req = new MemberCreateRequest("dup@loop.in", "nick", null, null, "KAKAO", "pid");
        given(memberRepository.existsByEmail("dup@loop.in")).willReturn(true);

        assertThatThrownBy(() -> memberService.regularSignUp(req))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.EMAIL_ALREADY_USED);
    }

    @Test
    @DisplayName("내 정보 조회: 존재 X → USER_NOT_FOUND")
    void getMyInfo_notFound() {
        var me = new CurrentUserDto(100L, "nope", "x@x", null, "ROLE_USER");
        given(memberRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyInfo(me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("닉네임 중복 확인: 중복이면 예외")
    void checkNickname_duplicate() {
        given(memberRepository.existsByNickname("dup")).willReturn(true);

        assertThatThrownBy(() -> memberService.checkNickname("dup"))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.NICKNAME_ALREADY_USED);
    }

    @Test
    @DisplayName("회원정보 수정: 이미지 업로드 성공 & 기존 삭제")
    void updateMember_uploadImage() throws Exception {
        var me = new CurrentUserDto(1L, "jun", "jun@loop.in", "https://old", "ROLE_USER");
        var member = Member.builder().id(1L).email("jun@loop.in").nickname("jun").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("newNick")).willReturn(false);
        given(s3Service.uploadImageFile(any(MultipartFile.class), eq("profile-image")))
                .willReturn("https://new");

        var req = new MemberUpdateRequest("newNick", null, null); // 실제 필드에 맞게 수정
        memberService.updateMember(req, mock(MultipartFile.class), me);

        verify(s3Service).deleteFile("https://old");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://new");
    }

    @Test
    @DisplayName("팔로우 요청: 자기 자신 팔로우 → CANNOT_FOLLOW_SELF")
    void followReq_self() {
        var me = new CurrentUserDto(10L, "me", "me@me", null, "ROLE_USER");
        assertThatThrownBy(() -> memberService.followReq(10L, me))
                .isInstanceOf(ServiceException.class)
                .extracting("returnCode").isEqualTo(ReturnCode.CANNOT_FOLLOW_SELF);
    }
}
