package com.loopone.loopinbe.domain.account.member.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.global.common.response.PageResponse;
import com.loopone.loopinbe.global.config.SecurityConfig;
import com.loopone.loopinbe.global.config.WebConfig;
import com.loopone.loopinbe.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = MemberController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        JwtAuthenticationFilter.class,     // ← 보안 필터 컴포넌트 제외
                        SecurityConfig.class,              // ← 전역 보안 설정 클래스를 쓰고 있다면 같이 제외
                        WebConfig.class                    // ← 전역 WebMvcConfigurer가 보안/리졸버를 끌어오면 제외
                })
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MemberControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MemberService memberService;
    @MockitoBean CurrentUserArgumentResolver currentUserArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(currentUserArgumentResolver.supportsParameter(any()))
                .willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(new CurrentUserDto(
                        1L, "jun@loop.in", null, "jun", "010-0000-0000",
                        Member.Gender.MALE, LocalDate.of(2000,1,1),
                        null, Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                        Member.OAuthProvider.GOOGLE, "provider-id"
                ));
    }

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
        verify(memberService).deleteMember(any(CurrentUserDto.class), "accessToken");
    }

    // --- 성공 케이스: 회원 검색 (page/size 기본 파라미터 포함) ---
    @Test
    @DisplayName("GET /rest-api/v1/member/search?keyword=kw&page=0&size=2 → 200 OK")
    void searchMemberInfo_success() throws Exception {
        var m1 = new MemberResponse(3L, "gangneung", null, 0L, 0L, 3L);
        var m2 = new MemberResponse(4L, "busan",     null, 0L, 0L, 4L);

        var pageable = PageRequest.of(0, 15); // 기본값
        Page<MemberResponse> springPage = new PageImpl<>(List.of(m1, m2), pageable, 2);
        PageResponse<MemberResponse> pageResponse = PageResponse.of(springPage);

        given(memberService.searchMemberInfo(
                any(Pageable.class), anyString(), any(CurrentUserDto.class))
        ).willReturn(pageResponse);

        // when & then
        mvc.perform(get("/rest-api/v1/member/search")
                        .param("keyword", "a"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // data: 배열 크기와 닉네임 구성 확인
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].nickname",
                        containsInAnyOrder("gangneung", "busan")))
                // page: 루트 레벨 메타 확인
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(15))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.first").value(true))
                .andExpect(jsonPath("$.page.last").value(true))
                .andExpect(jsonPath("$.page.hasNext").value(false));
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
}
