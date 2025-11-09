package com.loopone.loopinbe.domain.account.member.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberController.class)
@Import(MemberControllerTest.TestConfig.class) // @CurrentUser 리졸버 주입
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MemberService memberService;

    // --- 성공 케이스: 본인 회원정보 조회 ---
    @Test
    @DisplayName("GET /rest-api/v1/member → 200 OK & ApiResponse 래퍼")
    void getMyInfo_success() throws Exception {
        var resp = new MemberResponse(1L, "jun", "https://img", 10L, 5L, 777L);
        given(memberService.getMyInfo(any(CurrentUserDto.class))).willReturn(resp);

        mvc.perform(get("/rest-api/v1/member"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("jun"));
    }

    // --- 성공 케이스: 본인 상세회원정보 조회 ---
    @Test
    @DisplayName("GET /rest-api/v1/member/detail → 200 OK")
    void getMyDetailInfo_success() throws Exception {
        var detail = DetailMemberResponse.builder()
                .id(1L).email("jun@loop.in").nickname("jun").chatRoomId(123L)
                .followMemberCount(10L).followedMemberCount(5L)
                .followList(List.of()).followedList(List.of()).followReqList(List.of()).followRecList(List.of())
                .build();
        given(memberService.getMyDetailInfo(any(CurrentUserDto.class))).willReturn(detail);

        mvc.perform(get("/rest-api/v1/member/detail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("jun"))
                .andExpect(jsonPath("$.data.chatRoomId").value(123));
    }

    // --- 성공 케이스: 다른 사용자의 회원정보 조회 ---
    @Test
    @DisplayName("GET /rest-api/v1/member/{memberId} → 200 OK")
    void getMemberInfo_success() throws Exception {
        var resp = new MemberResponse(2L, "koo", "https://img2", 100L, 50L, 999L);
        given(memberService.getMemberInfo(2L)).willReturn(resp);

        mvc.perform(get("/rest-api/v1/member/{memberId}", 2L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.nickname").value("koo"))
                .andExpect(jsonPath("$.data.chatRoomId").value(999));
    }

    // --- 성공 케이스: 다른 사용자의 상세회원정보 조회 ---
    @Test
    @DisplayName("GET /rest-api/v1/member/detail/{memberId} → 200 OK")
    void getDetailMemberInfo_success() throws Exception {
        var detail = DetailMemberResponse.builder()
                .id(2L)
                .email("koo@loop.in")
                .nickname("koo")
                .profileImageUrl("https://img2")
                .followMemberCount(100L)     // 내가 팔로우하는 수
                .followedMemberCount(50L)    // 나를 팔로우하는 수
                .followList(List.of())       // 간단히 빈 리스트로
                .followedList(List.of())
                .followReqList(List.of())
                .followRecList(List.of())
                .chatRoomId(999L)
                .build();
        given(memberService.getDetailMemberInfo(2L)).willReturn(detail);

        mvc.perform(get("/rest-api/v1/member/detail/{memberId}", 2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.email").value("koo@loop.in"))
                .andExpect(jsonPath("$.data.nickname").value("koo"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://img2"))
                .andExpect(jsonPath("$.data.followMemberCount").value(100))
                .andExpect(jsonPath("$.data.followedMemberCount").value(50))
                .andExpect(jsonPath("$.data.chatRoomId").value(999));
    }

    // --- 검증/파라미터: 닉네임 중복 확인 ---
    @Test
    @DisplayName("GET /rest-api/v1/member/available?nickname=foo → 200 OK & 메시지")
    void checkNickname_available() throws Exception {
        mvc.perform(get("/rest-api/v1/member/available").param("nickname", "unique-nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("사용 가능한 닉네임입니다."));
        verify(memberService).checkNickname("unique-nick");
    }

    // --- 멀티파트 + ModelAttribute 검증: 회원정보 수정 ---
    @Test
    @DisplayName("PATCH /rest-api/v1/member (multipart) → 200 OK")
    void updateMemberInfo_success() throws Exception {
        var file = new MockMultipartFile("imageFile", "p.png",
                MediaType.IMAGE_PNG_VALUE, "png".getBytes());

        // @ModelAttribute MemberUpdateRequest(필드명은 실제 DTO에 맞게)
        mvc.perform(multipart("/rest-api/v1/member")
                        .file(file)
                        .param("nickname", "newNick")
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk());

        verify(memberService).updateMember(any(MemberUpdateRequest.class), any(), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 회원탈퇴 ---
    @Test
    @DisplayName("DELETE /rest-api/v1/member → 200 OK")
    void deleteMember_success() throws Exception {
        mvc.perform(delete("/rest-api/v1/member"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).deleteMember(any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 회원 검색 (page/size 기본 파라미터 포함) ---
    @Test
    @DisplayName("GET /rest-api/v1/member/search?keyword=kw&page=0&size=2 → 200 OK")
    void searchMemberInfo_success() throws Exception {
        var list = List.of(
                new MemberResponse(10L, "alice", "https://a", 1L, 2L, 11L),
                new MemberResponse(20L, "bob", "https://b", 3L, 4L, 22L)
        );
        given(memberService.searchMemberInfo(any(Pageable.class), eq("kw"), any(CurrentUserDto.class)))
                .willReturn((PageResponse<MemberResponse>) list);

        mvc.perform(get("/rest-api/v1/member/search")
                        .param("keyword", "kw")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].nickname").value("alice"))
                .andExpect(jsonPath("$.data[1].id").value(20))
                .andExpect(jsonPath("$.data[1].nickname").value("bob"));

        verify(memberService).searchMemberInfo(any(Pageable.class), eq("kw"), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로우 요청하기 ---
    @Test
    @DisplayName("POST /rest-api/v1/member/follow/{memberId} → 200 OK")
    void followReq_success() throws Exception {
        mvc.perform(post("/rest-api/v1/member/follow/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).followReq(eq(7L), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로우 요청 취소하기 ---
    @Test
    @DisplayName("DELETE /rest-api/v1/member/follow/{memberId} → 200 OK")
    void cancelFollowReq_success() throws Exception {
        mvc.perform(delete("/rest-api/v1/member/follow/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).cancelFollowReq(eq(7L), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로우 요청 수락하기 ---
    @Test
    @DisplayName("POST /rest-api/v1/member/followReq/{memberId} → 200 OK")
    void acceptFollowReq_success() throws Exception {
        mvc.perform(post("/rest-api/v1/member/followReq/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).acceptFollowReq(eq(7L), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로우 요청 거절하기 ---
    @Test
    @DisplayName("DELETE /rest-api/v1/member/followReq/{memberId} → 200 OK")
    void refuseFollowReq_success() throws Exception {
        mvc.perform(delete("/rest-api/v1/member/followReq/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).refuseFollowReq(eq(7L), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로우 취소하기 ---
    @Test
    @DisplayName("DELETE /rest-api/v1/member/followMember/{memberId} → 200 OK")
    void cancelFollow_success() throws Exception {
        mvc.perform(delete("/rest-api/v1/member/followMember/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).cancelFollow(eq(7L), any(CurrentUserDto.class));
    }

    // --- 성공 케이스: 팔로워 목록에서 해당 유저 삭제하기 ---
    @Test
    @DisplayName("DELETE /rest-api/v1/member/followed/{memberId} → 200 OK")
    void removeFollowed_success() throws Exception {
        mvc.perform(delete("/rest-api/v1/member/followed/{memberId}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(memberService).removeFollowed(eq(7L), any(CurrentUserDto.class));
    }

    /**
     * 테스트 전용 ArgumentResolver: @CurrentUser 주입
     * 실제 시큐리티 필터/토큰을 피하면서도 Controller 시그니처 유지
     */
    static class TestConfig {
        @Bean
        HandlerMethodArgumentResolver currentUserArgumentResolver() {
            return new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter p) {
                    return p.hasParameterAnnotation(CurrentUser.class)
                            || p.getParameterType().equals(CurrentUserDto.class);
                }

                @Override
                public Object resolveArgument(
                        MethodParameter parameter,
                        ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest,
                        WebDataBinderFactory binderFactory
                ) {
                    return new CurrentUserDto(
                            1L,                                 // id
                            "jun@loop.in",                      // email
                            null,                               // password (불필요 → null)
                            "jun",                              // nickname
                            "010-0000-0000",                    // phone
                            Member.Gender.MALE,                 // gender
                            LocalDate.of(2000, 1, 1),           // birthday
                            null,                      // profileImageUrl
                            Member.State.NORMAL,                // state
                            Member.MemberRole.ROLE_USER,             // role
                            Member.OAuthProvider.GOOGLE,        // provider
                            "provider-id"                       // providerId
                    );
                }
            };
        }
    }
}
