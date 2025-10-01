package com.loopone.loopinbe.domain.loop.loop.dto.res;

import com.loopone.loopinbe.domain.loop.checkList.dto.res.LoopCheckListResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoopWithCheckListResponse {
    private LoopResponse loop;
    private List<LoopCheckListResponse> checkList;
}
