package com.loopone.loopinbe.domain.loop.loop.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class LoopPage {
    // 기본 page, size
    private int page = 0;
    private int size = 20;
    @Getter
    private static final int maxPageSize = 20;
}
