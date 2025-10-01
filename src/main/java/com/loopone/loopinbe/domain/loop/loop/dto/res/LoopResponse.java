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
    //private List<LoopChecklistResponseDTO> checklists;
}
