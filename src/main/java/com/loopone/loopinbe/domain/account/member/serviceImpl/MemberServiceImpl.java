package com.loopone.loopinbe.domain.account.member.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letzgo.LetzgoBe.domain.account.auth.currentUser.CurrentUserDto;
import com.letzgo.LetzgoBe.domain.account.auth.service.AuthService;
import com.letzgo.LetzgoBe.domain.account.member.dto.req.MemberRequest;
import com.letzgo.LetzgoBe.domain.account.member.dto.res.DetailMemberResponse;
import com.letzgo.LetzgoBe.domain.account.member.dto.res.MemberResponse;
import com.letzgo.LetzgoBe.domain.account.member.entity.Member;
import com.letzgo.LetzgoBe.domain.account.member.entity.MemberFollow;
import com.letzgo.LetzgoBe.domain.account.member.entity.MemberFollowReq;
import com.letzgo.LetzgoBe.domain.account.member.entity.MemberPage;
import com.letzgo.LetzgoBe.domain.account.member.mapper.MemberMapper;
import com.letzgo.LetzgoBe.domain.account.member.repository.MemberFollowRepository;
import com.letzgo.LetzgoBe.domain.account.member.repository.MemberFollowReqRepository;
import com.letzgo.LetzgoBe.domain.account.member.repository.MemberRepository;
import com.letzgo.LetzgoBe.domain.account.member.service.MemberService;
import com.letzgo.LetzgoBe.domain.chat.chatMessage.service.ChatMessageService;
import com.letzgo.LetzgoBe.domain.chat.chatRoom.service.ChatRoomService;
import com.letzgo.LetzgoBe.domain.community.comment.service.CommentService;
import com.letzgo.LetzgoBe.domain.community.post.service.PostService;
import com.letzgo.LetzgoBe.domain.notification.entity.Notification;
import com.letzgo.LetzgoBe.global.common.response.PageResponse;
import com.letzgo.LetzgoBe.global.exception.ReturnCode;
import com.letzgo.LetzgoBe.global.exception.ServiceException;
import com.letzgo.LetzgoBe.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final MemberFollowReqRepository memberFollowReqRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final S3Service s3Service;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MemberMapper memberMapper;

    // 회원가입
    @Override
    @Transactional
    public Member signup(MemberRequest memberRequest) {
        if (memberRepository.existsByEmail((memberRequest.getEmail()))) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
        // 비밀번호가 없으면 null로 처리하거나 다른 처리를 할 수 있습니다.
        String encodedPassword = memberRequest.getPassword() != null ? passwordEncoder.encode(memberRequest.getPassword()) : null;
        Member member = Member.builder()
                .name(memberRequest.getName())
                .nickname(memberRequest.getNickname())
                .phone(memberRequest.getPhone())
                .email(memberRequest.getEmail())
                .password(encodedPassword)  // 인코딩된 비밀번호 저장
                .gender(memberRequest.getGender())
                .birthday(memberRequest.getBirthday())
                .build();
        memberRepository.save(member);
        return member;
    }

    // 본인 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(CurrentUserDto currentUser) {
        Member member = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toMemberResponse(member);
    }

    // 본인 상세회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public DetailMemberResponse getMyDetailInfo(CurrentUserDto currentUser){
        Member member = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberResponse(member);
    }

    // 다른 멤버의 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toMemberResponse(member);
    }

    // 다른 멤버의 상세회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public DetailMemberResponse getDetailMemberInfo(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        return memberMapper.toDetailMemberResponse(member);
    }

    // 회원정보 수정
    @Override
    @Transactional
    public void updateMember(MemberRequest memberRequest, MultipartFile imageFile, CurrentUserDto currentUser) {
        /// 기존 이미지 삭제 후 입력 받은 이미지 S3에 저장
        String imageUrl = currentUser.getProfileImageUrl(); // 기본적으로 기존 이미지 URL을 사용
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 없으면 바로 새로운 이미지 저장
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl);
            try {
                imageUrl = s3Service.uploadFile(imageFile, "profile-image");
            } catch (IOException e) {
                throw new ServiceException(ReturnCode.INTERNAL_ERROR);
            }
        } else {
            // imageFile이 없으면 기존 이미지가 있다면 삭제한다
            if (imageUrl != null && !imageUrl.isEmpty()) s3Service.deleteFile(imageUrl); // 기존 이미지 삭제
            imageUrl = null;
        }
        if (memberRequest.getName() != null) currentUser.setName(memberRequest.getName());
        if (memberRequest.getNickname() != null) currentUser.setNickname(memberRequest.getNickname());
        if (memberRequest.getPhone() != null) currentUser.setPhone(memberRequest.getPhone());
        if (memberRequest.getEmail() != null) currentUser.setEmail(memberRequest.getEmail());
        if (memberRequest.getPassword() != null) {
            currentUser.setPassword(BCrypt.hashpw(memberRequest.getPassword(), BCrypt.gensalt()));
        }
        if (memberRequest.getGender() != null) currentUser.setGender(memberRequest.getGender());
        if (memberRequest.getBirthday() != null) currentUser.setBirthday(memberRequest.getBirthday());
        currentUser.setProfileImageUrl(imageUrl);
        // LoginUserDto를 Member 엔티티로 변환
        Member memberEntity = memberMapper.toMember(currentUser);
        memberRepository.save(memberEntity);
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void deleteMember(CurrentUserDto currentUser) {
        // refreshToken 삭제
        authService.logout(currentUser);
        // DB에서 회원 조회
        Member memberEntity = memberRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        // 연관된 데이터 삭제
        commentService.deleteMembersAllComments(currentUser.getId());
        postService.deleteMembersAllPosts(currentUser.getId());
        chatMessageService.deleteMembersAllChatMessages(currentUser.getId());
        chatRoomService.leaveAllChatRooms(currentUser.getId());

        memberRepository.delete(memberEntity);
    }

    // 회원 검색하기
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> searchMemberInfo(Pageable pageable, String keyword){
        checkPageSize(pageable.getPageSize());
        Page<MemberResponse> members = memberRepository.findByKeyword(pageable, keyword);
        return PageResponse.of(members);
    }

    // 팔로우 요청하기
    @Override
    @Transactional
    public void followReq(Long memberId, CurrentUserDto currentUser){
        Member followReq = memberMapper.toMember(currentUser);
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
                .build();
        try {
            String message = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("follow-topic", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 팔로우 요청 취소하기
    @Override
    @Transactional
    public void cancelFollowReq(Long memberId, CurrentUserDto currentUser){
        Member followReq = memberMapper.toMember(currentUser);
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
        Member receiver = memberMapper.toMember(currentUser);
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
                .senderId(currentUser.getId())
                .senderNickname(currentUser.getNickname())
                .senderProfileUrl(currentUser.getProfileImageUrl())
                .receiverId(memberId)
                .objectId(memberFollow.getId())
                .content("님이 팔로우 요청을 수락하였습니다.")
                .targetObject(Notification.TargetObject.Follow)
                .build();
        try {
            String message = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("follow-topic", message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Notification: {}", e.getMessage());
        }
    }

    // 팔로우 요청 거절하기
    @Override
    @Transactional
    public void refuseFollowReq(Long memberId, CurrentUserDto currentUser){
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Member receiver = memberMapper.toMember(currentUser);
        MemberFollowReq memberFollowReq = memberFollowReqRepository.findByFollowReqAndFollowRec(requester, receiver)
                .orElseThrow(() -> new ServiceException(ReturnCode.REQUEST_NOT_FOUND));
        memberFollowReqRepository.delete(memberFollowReq);
    }

    // 팔로우 취소하기
    @Override
    @Transactional
    public void cancelFollow(Long memberId, CurrentUserDto currentUser){
        Member follow = memberMapper.toMember(currentUser);
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
        Member followed = memberMapper.toMember(currentUser);
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
