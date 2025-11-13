package com.loopone.loopinbe.domain.loop.loop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserArgumentResolver;
import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopGroupUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.enums.RepeatType;
import com.loopone.loopinbe.domain.loop.loop.service.LoopService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ApiV1LoopController.class,
        excludeFilters = { //WebConfig, SecurityConfig 제외하여 테스트
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        JwtAuthenticationFilter.class,
                        SecurityConfig.class,
                        WebConfig.class
                })
        },
        excludeAutoConfiguration = { //보안 관련 자동 설정 비활성화
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false) //모든 Spring Security 필터 비활성화
@ActiveProfiles("test")
class ApiV1LoopControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoopService loopService;

    @MockitoBean
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    private CurrentUserDto testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new CurrentUserDto(
                1L, "test@loop.in", null, "testUser",
                null, null, null, null,
                Member.State.NORMAL, Member.MemberRole.ROLE_USER,
                Member.OAuthProvider.NONE, null
        );

        given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(testUser);
    }

    //루프 생성 테스트
    @Test
    @DisplayName("루프 생성 API - POST /rest-api/v1/loops")
    void addLoop_Success() throws Exception {
        //given
        //컨트롤러에 전송할 Request 생성
        LoopCreateRequest request = new LoopCreateRequest(
                "루프 생성", "내용", RepeatType.NONE,
                LocalDate.now(), null, null, null, List.of("체크리스트1")
        );
        //createLoop 메서드가 호출되면, 아무것도 하지 않도록 설정
        willDoNothing().given(loopService).createLoop(any(LoopCreateRequest.class), any(CurrentUserDto.class));

        //when & then
        mvc.perform(post("/rest-api/v1/loops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"));

        verify(loopService).createLoop(any(LoopCreateRequest.class), any(CurrentUserDto.class));
    }

    //루프 상세 조회 테스트
    @Test
    @DisplayName("루프 상세 조회 API - GET /rest-api/v1/loops/{loopId}")
    void getDetailLoop_Success() throws Exception {
        //given
        Long loopId = 1L;
        LoopDetailResponse mockResponse = LoopDetailResponse.builder()
                .id(loopId)
                .title("루프 상세 조회")
                .content("상세 내용")
                .progress(50.0)
                .loopRule(null)
                .build();

        given(loopService.getDetailLoop(eq(loopId), any(CurrentUserDto.class))).willReturn(mockResponse);

        //when & then
        mvc.perform(get("/rest-api/v1/loops/{loopId}", loopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(loopId))
                .andExpect(jsonPath("$.data.title").value("루프 상세 조회"))
                .andExpect(jsonPath("$.data.progress").value(50.0));

        // LoopService의 getDetailLoop 메서드가 올바른 인자들로 1회 호출되었는지 검증
        verify(loopService).getDetailLoop(eq(loopId), any(CurrentUserDto.class));
    }

    //날짜별 루프 조회 테스트
    @Test
    @DisplayName("날짜별 루프 조회 API - GET /rest-api/v1/loops/date/{loopDate}")
    void getDailyLoops_Success() throws Exception {
        //given
        LocalDate date = LocalDate.of(2025, 11, 13);
        DailyLoopsResponse mockResponse = DailyLoopsResponse.builder()
                .totalProgress(100.0)
                .loops(List.of())
                .build();

        given(loopService.getDailyLoops(eq(date), any(CurrentUserDto.class))).willReturn(mockResponse);

        //when & then
        mvc.perform(get("/rest-api/v1/loops/date/{loopDate}", "2025-11-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProgress").value(100.0)); // 응답 JSON의 data.totalProgress 확인

        verify(loopService).getDailyLoops(eq(date), any(CurrentUserDto.class));
    }

    //단일 루프 수정 테스트
    @Test
    @DisplayName("단일 루프 수정 API - PUT /rest-api/v1/loops/{loopId}")
    void updateLoop_Success() throws Exception {
        //given
        Long loopId = 1L;
        LoopUpdateRequest request = new LoopUpdateRequest("수정된 제목", null, null, List.of());

        willDoNothing().given(loopService).updateLoop(eq(loopId), any(LoopUpdateRequest.class), any(CurrentUserDto.class));

        //when & then
        mvc.perform(put("/rest-api/v1/loops/{loopId}", loopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loopService).updateLoop(eq(loopId), any(LoopUpdateRequest.class), any(CurrentUserDto.class));
    }

    //루프 그룹 수정 테스트
    @Test
    @DisplayName("루프 그룹 수정 API - PUT /rest-api/v1/loops/group/{loopRuleId}")
    void updateGroupLoop_Success() throws Exception {
        //given
        Long loopRuleId = 10L;
        LoopGroupUpdateRequest request = new LoopGroupUpdateRequest(
                "그룹 제목 수정", "그룹 내용 수정", RepeatType.WEEKLY,
                null, List.of(DayOfWeek.SATURDAY), LocalDate.now(), null, List.of("CL New")
        );

        willDoNothing().given(loopService).updateLoopGroup(eq(loopRuleId), any(LoopGroupUpdateRequest.class), any(CurrentUserDto.class));

        //when & then
        mvc.perform(put("/rest-api/v1/loops/group/{loopRuleId}", loopRuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loopService).updateLoopGroup(eq(loopRuleId), any(LoopGroupUpdateRequest.class), any(CurrentUserDto.class));
    }

    //단일 루프 삭제 테스트
    @Test
    @DisplayName("단일 루프 삭제 API - DELETE /rest-api/v1/loops/{loopId}")
    void deleteLoop_Success() throws Exception {
        //given
        Long loopId = 1L;
        willDoNothing().given(loopService).deleteLoop(eq(loopId), any(CurrentUserDto.class));

        //when & then
        mvc.perform(delete("/rest-api/v1/loops/{loopId}", loopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loopService).deleteLoop(eq(loopId), any(CurrentUserDto.class));
    }

    //루프 그룹 삭제 테스트
    @Test
    @DisplayName("루프 그룹 삭제 API - DELETE /rest-api/v1/loops/group/{loopRuleId}")
    void deleteLoopGroup_Success() throws Exception {
        //given
        Long loopRuleId = 10L;
        willDoNothing().given(loopService).deleteLoopGroup(eq(loopRuleId), any(CurrentUserDto.class));

        //when & then
        mvc.perform(delete("/rest-api/v1/loops/group/{loopRuleId}", loopRuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loopService).deleteLoopGroup(eq(loopRuleId), any(CurrentUserDto.class));
    }
}