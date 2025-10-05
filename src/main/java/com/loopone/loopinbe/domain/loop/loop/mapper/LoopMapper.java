package com.loopone.loopinbe.domain.loop.loop.mapper;

import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoopMapper {
    // Loop 엔티티를 LoopSimpleResponse로 변환
    @Mapping(target = "progress", source = "loop.loopChecklists", qualifiedByName = "calculateProgress")
    LoopSimpleResponse toSimpleResponse(Loop loop);

    // Loop 엔티티를 LoopDetailResponse로 변환
    @Mapping(target = "progress", source = "loop.loopChecklists", qualifiedByName = "calculateProgress")
    @Mapping(target = "checklists", source = "loopChecklists")
    LoopDetailResponse toDetailResponse(Loop loop);

    // LoopChecklist 엔티티를 LoopChecklistResponse로 변환
    LoopChecklistResponse toChecklistResponse(LoopChecklist checklist);

    // 진행률(progress)을 계산하는 헬퍼 메서드
    @Named("calculateProgress")
    default double calculateProgress(List<LoopChecklist> checklists) {
        if (checklists == null || checklists.isEmpty()) {
            return 0.0;
        }
        long completedCount = checklists.stream().filter(LoopChecklist::getCompleted).count();
        return ((double) completedCount / checklists.size()) * 100.0;
    }
}
