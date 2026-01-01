package com.loopone.loopinbe.domain.loop.ai.dto.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AiType {
    OPEN_AI("open_ai"),
    GEMINI("gemini");

    private final String name;
}
