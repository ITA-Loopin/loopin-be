package com.loopone.loopinbe.domain.loop.loop.dto.res;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoopResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDate loopDate;
    private double progress;
    //TODO: 체크리스트 목록으로 응답해줘야함.
    //private List<LoopChecklistResponseDTO> checklists;
}
