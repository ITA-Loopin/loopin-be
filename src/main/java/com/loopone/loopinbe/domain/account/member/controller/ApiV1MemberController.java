package com.loopone.loopinbe.domain.account.member.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.MemberPage;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/member")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping
    public ApiResponse<Void> signup(@RequestBody @Valid MemberRequest memberRequest) {
        memberService.signup(memberRequest);
        return ApiResponse.success();
    }

    // 본인 회원정보 조회
    @GetMapping
    public ApiResponse<MemberResponse> getMyInfo(@CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMyInfo(currentUser));
    }

    // 본인 상세회원정보 조회
    @GetMapping("/detail")
    public ApiResponse<DetailMemberResponse> getMyDetailInfo(@CurrentUser CurrentUserDto currentUser) {
        return ApiResponse.success(memberService.getMyDetailInfo(currentUser));
    }

    // 다른 멤버의 회원정보 조회
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getMemberInfo(@PathVariable("memberId") Long memberId) {
        return ApiResponse.success(memberService.getMemberInfo(memberId));
    }

    // 다른 멤버의 상세회원정보 조회
    @GetMapping("/detail/{memberId}")
    public ApiResponse<DetailMemberResponse> getDetailMemberInfo(@PathVariable("memberId") Long memberId) {
        return ApiResponse.success(memberService.getDetailMemberInfo(memberId));
    }

    // 회원정보 수정
    @PatchMapping
    public ApiResponse<Void> updateMemberInfo(@RequestPart(value = "memberForm") @Valid MemberRequest memberRequest,
                                                @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                                                @CurrentUser CurrentUserDto currentUser) {
        memberService.updateMember(memberRequest, imageFile, currentUser);
        return ApiResponse.success();
    }

    // 회원탈퇴
    @DeleteMapping
    public ApiResponse<Void> deleteMember(@CurrentUser CurrentUserDto currentUser) {
        memberService.deleteMember(currentUser);
        return ApiResponse.success();
    }

    // 회원 검색하기
    @GetMapping("/search")
    public ApiResponse<List<MemberResponse>> searchMemberInfo(@ModelAttribute MemberPage request, @RequestParam(value = "keyword") String keyword) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(memberService.searchMemberInfo(pageable, keyword));
    }

    // 팔로우 요청하기
    @PostMapping("/follow/{memberId}")
    public ApiResponse<Void> followReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.followReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 취소하기
    @DeleteMapping("/follow/{memberId}")
    public ApiResponse<Void> cancelFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.cancelFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 수락하기
    @PostMapping("/followReq/{memberId}")
    public ApiResponse<Void> acceptFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.acceptFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 요청 거절하기
    @DeleteMapping("/followReq/{memberId}")
    public ApiResponse<Void> refuseFollowReq(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.refuseFollowReq(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로우 취소하기
    @DeleteMapping("/followMember/{memberId}")
    public ApiResponse<Void> cancelFollow(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.cancelFollow(memberId, currentUser);
        return ApiResponse.success();
    }

    // 팔로워 목록에서 해당 유저 삭제하기
    @DeleteMapping("/followed/{memberId}")
    public ApiResponse<Void> removeFollowed(@PathVariable("memberId") Long memberId, @CurrentUser CurrentUserDto currentUser) {
        memberService.removeFollowed(memberId, currentUser);
        return ApiResponse.success();
    }
}
