package com.loopone.loopinbe.domain.account.member.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.auth.dto.AuthPayload;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollow;
import com.loopone.loopinbe.domain.account.member.entity.MemberFollowReq;
import com.loopone.loopinbe.domain.account.member.entity.MemberPage;
import com.loopone.loopinbe.domain.account.member.converter.MemberConverter;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberFollowReqRepository;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.domain.chat.chatRoom.service.ChatRoomService;
import com.loopone.loopinbe.domain.notification.entity.Notification;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import com.loopone.loopinbe.global.kafka.event.auth.AuthEventPublisher;
import com.loopone.loopinbe.global.kafka.event.notification.NotificationEventPublisher;
import com.loopone.loopinbe.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberFollowReqRepository memberFollowReqRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final S3Service s3Service;
    private final MemberConverter memberConverter;
    private final ChatRoomService chatRoomService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AuthEventPublisher authEventPublisher;

    // 회원가입
    @Override
    @Transactional
    public Member regularSignUp(MemberCreateRequest memberCreateRequest) {
        if (memberRepository.existsByEmail((memberCreateRequest.getEmail()))) {
            throw new ServiceException(ReturnCode.EMAIL_ALREADY_USED);
        }
        if (memberRepository.existsByNickname(memberCreateRequest.getNickname())) {
            throw new ServiceException(ReturnCode.NICKNAME_ALREADY_USED);
        }
        Member member = Member.builder()
                .email(memberCreateRequest.getEmail())
//                .password(encodedPassword)
                .nickname(memberCreateRequest.getNickname())
//                .phone(memberCreateRequest.getPhone())// 인코딩된 비밀번호 저장
//                .gender(memberCreateRequest.getGender())
//                .birthday(memberCreateRequest.getBirthday())
                .oAuthProvider(memberCreateRequest.getProvider())
                .providerId(memberCreateRequest.getProviderId())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 본인 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(CurrentUserDto currentUser) {
        Member member = memberRepository.findById(currentUser.id())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConverter.toMemberResponse(member);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public DetailMemberResponse getMyDetailInfo(CurrentUserDto currentUser){
        Member member = memberRepository.findById(currentUser.id())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConverter.toDetailMemberResponse(member);
    }

    // 다른 멤버의 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConverter.toMemberResponse(member);
    }

    // 다른 멤버의 상세회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public DetailMemberResponse getDetailMemberInfo(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberConverter.toDetailMemberResponse(member);
    }

    // 닉네임 중복 확인
    @Override
    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new ServiceException(ReturnCode.NICKNAME_ALREADY_USED);
        }
    }

    // 회원정보 수정
    @Override
    @Transactional
    public void updateMember(MemberUpdateRequest memberUpdateRequest, MultipartFile imageFile, CurrentUserDto currentUser) {
        Member member = memberRepository.findById(currentUser.id())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        if (memberRepository.existsByNickname(memberUpdateRequest.nickname())) {
            throw new ServiceException(ReturnCode.NICKNAME_ALREADY_USED);
        }
        // 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = currentUser.profileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl);
            try {
                imageUrl = s3Service.uploadImageFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            imageUrl = null;
        }
        member.update(memberUpdateRequest, imageUrl, null);
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void deleteMember(CurrentUserDto currentUser, String accessToken) {
        // DB에서 회원 조회
        Member memberEntity = memberRepository.findById(currentUser.id())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 연관된 데이터 삭제
        chatRoomService.leaveAllChatRooms(currentUser.id());
        // 회원삭제
        memberRepository.delete(memberEntity);
        // 로그아웃
        AuthPayload payload = new AuthPayload(
                java.util.UUID.randomUUID().toString(),
                currentUser.id(),
                accessToken
        );
        authEventPublisher.publishLogoutAfterCommit(payload);
    }

    // 회원 검색하기
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> searchMemberInfo(Pageable pageable, String keyword, CurrentUserDto currentUser) {
        checkPageSize(pageable.getPageSize());
        Page<MemberResponse> members = memberRepository.findByKeyword(pageable, keyword, currentUser.id());
        return PageResponse.of(members);
    }

    // 팔로우 요청하기
    @Override
    @Transactional
    public void followReq(Long memberId, CurrentUserDto currentUser){
        if (Objects.equals(memberId, currentUser.id())) {
            throw new ServiceException(ReturnCode.CANNOT_FOLLOW_SELF);
        }
        Member followReq = memberConverter.toMember(currentUser);
        Member followRec = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 기존 팔로우 여부 확인
        boolean already_follow = memberFollowRepository.existsByFollowAndFollowed(followReq, followRec);
        if (already_follow) {
            throw new ServiceException(ReturnCode.ALREADY_FOLLOW);
        }
        // 중복 요청 방지
        boolean already_requested = memberFollowReqRepository.existsByFollowReqAndFollowRec(followReq, followRec);
        if (already_requested) {
            throw new ServiceException(ReturnCode.ALREADY_REQUESTED);
        }
        MemberFollowReq memberFollowReq = MemberFollowReq.builder()
                .followReq(followReq)
                .followRec(followRec)
                .build();
        memberFollowReqRepository.save(memberFollowReq);
        // 팔로우 요청 이벤트 생성
        Notification notification = Notification.builder()
                .senderId(followReq.getId())
                .senderNickname(followReq.getNickname())
                .senderProfileUrl(followReq.getProfileImageUrl())
                .receiverId(followRec.getId())
                .objectId(memberFollowReq.getId())
                .content("님이 팔로우를 요청하였습니다.")
                .targetObject(Notification.TargetObject.Follow)
                .createdAt(LocalDateTime.now())
                .build();
        notificationEventPublisher.publishFollowRequest(notification);
    }

    // 팔로우 요청 취소하기
    @Override
    @Transactional
    public void cancelFollowReq(Long memberId, CurrentUserDto currentUser){
        Member followReq = memberConverter.toMember(currentUser);
        Member followRec = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(followReq, followRec)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 요청 수락하기
    @Override
    @Transactional
    public void acceptFollowReq(Long memberId, CurrentUserDto currentUser){
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberConverter.toMember(currentUser);
        MemberFollowReq followReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(followReq);
        MemberFollow memberFollow = MemberFollow.builder()
                .follow(requester)
                .followed(receiver)
                .build();
        memberFollowRepository.save(memberFollow);
        // 팔로우 수락 이벤트 생성
        Notification notification = Notification.builder()
                .senderId(currentUser.id())
                .senderNickname(currentUser.nickname())
                .senderProfileUrl(currentUser.profileImageUrl())
                .receiverId(memberId)
                .objectId(memberFollow.getId())
                .content("님이 팔로우 요청을 수락하였습니다.")
                .targetObject(Notification.TargetObject.Follow)
                .createdAt(LocalDateTime.now())
                .build();
        notificationEventPublisher.publishFollowRequest(notification);
    }

    // 팔로우 요청 거절하기
    @Override
    @Transactional
    public void refuseFollowReq(Long memberId, CurrentUserDto currentUser){
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberConverter.toMember(currentUser);
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 취소하기
    @Override
    @Transactional
    public void cancelFollow(Long memberId, CurrentUserDto currentUser){
        Member follow = memberConverter.toMember(currentUser);
        Member followed = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        MemberFollow memberFollow = memberFollowRepository.findByFollowAndFollowed(follow, followed)
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOW_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
    }

    // 팔로워 목록에서 해당 유저 삭제하기
    @Override
    @Transactional
    public void removeFollowed(Long memberId, CurrentUserDto currentUser){
        Member follow = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member followed = memberConverter.toMember(currentUser);
        MemberFollow memberFollow = memberFollowRepository.findByFollowAndFollowed(follow, followed)
                .orElseThrow(() -> new ServiceException(ReturnCode.FOLLOWER_NOT_FOUND));
        memberFollowRepository.delete(memberFollow);
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }
}
