package com.loopone.loopinbe.domain.loop.ai.serviceImpl;

import com.loopone.loopinbe.domain.loop.ai.dto.AiPayload;
import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import com.loopone.loopinbe.domain.loop.ai.service.AiProvider;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiRoute {
    private final List<AiProvider> providers;

    public AiRoute(
            List<AiProvider> providers
    ) {
        this.providers = providers;
    }

    public RecommendationsLoop route(AiPayload payload) {
        for (AiProvider provider : providers) {
            try {
                return provider.callOpenAi(payload);
            } catch (Exception e) {
                log.warn("{} 사용 불가", provider.getName());
            }
        }
        throw new ServiceException(ReturnCode.OPEN_AI_INTERNAL_ERROR);
    }
}
