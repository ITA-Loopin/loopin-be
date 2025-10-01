package com.loopone.loopinbe.domain.loop.loop.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopRequest {
    private String title;
    private LocalDate loopDate;
    private String content;
    private List<String> checklists;
}
