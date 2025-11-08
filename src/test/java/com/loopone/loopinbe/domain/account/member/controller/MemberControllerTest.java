package com.loopone.loopinbe.domain.account.member.controller;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUser;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.dto.req.MemberUpdateRequest;
import com.loopone.loopinbe.domain.account.member.dto.res.DetailMemberResponse;
import com.loopone.loopinbe.domain.account.member.dto.res.MemberResponse;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        var current = new CurrentUserDto(1L, "jun", "jun@loop.in", null, "ROLE_USER");
        var resp = new MemberResponse(1L, "jun", "https://img", 10L, 5L, 777L);
        given(memberService.getMyInfo(any(CurrentUserDto.class))).willReturn(resp);

        mvc.perform(get("/rest-api/v1/member"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // ApiResponse<String|Object> 규약에 맞춰 data 일부만 확인
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("jun"));
    }

    // --- 성공 케이스: 다른 사용자 상세 조회 ---
    @Test
    @DisplayName("GET /rest-api/v1/member/detail/{memberId} → 200 OK")
    void getDetailMemberInfo_success() throws Exception {
        var detail = new DetailMemberResponse(2L, "koo", "https://img2", "intro", 100L, 50L, null);
        given(memberService.getDetailMemberInfo(2L)).willReturn(detail);

        mvc.perform(get("/rest-api/v1/member/detail/{memberId}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.nickname").value("koo"));
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

    /**
     * 테스트 전용 ArgumentResolver: @CurrentUser 주입
     * 실제 시큐리티 필터/토큰을 피하면서도 Controller 시그니처 유지
     */
    static class TestConfig {
        @Bean
        HandlerMethodArgumentResolver currentUserArgumentResolver() {
            return new HandlerMethodArgumentResolver() {
                @Override public boolean supportsParameter(MethodParameter p) {
                    return p.hasParameterAnnotation(CurrentUser.class)
                            || p.getParameterType().equals(CurrentUserDto.class);
                }
                @Override
                public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                              NativeWebRequest w, org.springframework.web.bind.support.WebDataBinderFactory b) {
                    return new CurrentUserDto(1L, "jun", "jun@loop.in", null, "ROLE_USER");
                }
            };
        }
    }
}
