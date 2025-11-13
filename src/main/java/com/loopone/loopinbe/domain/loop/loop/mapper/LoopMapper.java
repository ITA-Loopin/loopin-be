package com.loopone.loopinbe.domain.loop.loop.mapper;

import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopCreateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.req.LoopGroupUpdateRequest;
import com.loopone.loopinbe.domain.loop.loop.dto.res.DailyLoopsResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopDetailResponse;
import com.loopone.loopinbe.domain.loop.loop.dto.res.LoopSimpleResponse;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.entity.LoopRule;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoopMapper {
    //Loop 엔티티를 LoopSimpleResponse로 변환
    @Mapping(target = "completed", source = "loop.loopChecklists", qualifiedByName = "isCompleted")
    @Mapping(target = "totalChecklists", source = "loop.loopChecklists", qualifiedByName = "calculateTotal")
    @Mapping(target = "completedChecklists", source = "loop.loopChecklists", qualifiedByName = "calculateCompleted")
    LoopSimpleResponse toSimpleResponse(Loop loop);

    //Loop 엔티티 리스트를 DailyLoopsResponse로 반환
    default DailyLoopsResponse toDailyLoopsResponse(List<Loop> loops) {
        if (loops == null || loops.isEmpty()) {
            //루프가 없을 경우 진행률 0%, 빈 리스트 반환
            return new DailyLoopsResponse(0.0, List.of());
        }

        //loop 엔티티를 toSimpleResponse로 변환
        List<LoopSimpleResponse> list = loops.stream().map(this::toSimpleResponse).toList();

        //전체 진행률 계산
        long count = list.stream().filter(LoopSimpleResponse::completed).count();
        double totalProgress = ((double) count / loops.size()) * 100.0;

        return new DailyLoopsResponse(totalProgress, list);
    }

    //Loop 엔티티를 LoopDetailResponse로 변환
    @Mapping(target = "progress", source = "loop.loopChecklists", qualifiedByName = "calculateProgress")
    @Mapping(target = "checklists", source = "loopChecklists")
    @Mapping(target = "loopRule", source = "loopRule")
    LoopDetailResponse toDetailResponse(Loop loop);

    @Mapping(target = "ruleId", source = "id")
    LoopDetailResponse.LoopRuleDTO loopRuleToLoopRuleDTO(LoopRule loopRule);

    //LoopChecklist 엔티티를 LoopChecklistResponse로 변환
    LoopChecklistResponse toChecklistResponse(LoopChecklist checklist);

    //LoopGroupUpdateRequest를 LoopCreateRequest로 변환
    LoopCreateRequest toLoopCreateRequest(LoopGroupUpdateRequest requestDTO);

    //진행률(progress)을 계산하는 헬퍼 메서드
    @Named("calculateProgress")
    default double calculateProgress(List<LoopChecklist> checklists) {
        if (checklists == null || checklists.isEmpty()) {
            return 0.0;
        }
        long completedCount = checklists.stream().filter(LoopChecklist::getCompleted).count();
        return ((double) completedCount / checklists.size()) * 100.0;
    }

    //루프 자체의 완료 여부를 확인하는 헬퍼 메서드
    @Named("isCompleted")
    default boolean isCompleted(List<LoopChecklist> checklists) {
        if (checklists == null || checklists.isEmpty()) return false;
        return checklists.stream().allMatch(LoopChecklist::getCompleted);
    }

    //체크리스트 전체 개수를 계산하는 헬퍼 메서드
    @Named("calculateTotal")
    default int calculateTotal(List<LoopChecklist> checklists) {
        return checklists != null ? checklists.size() : 0;
    }

    //체크리스트 완료 개수를 계산하는 헬퍼 메서드
    @Named("calculateCompleted")
    default int calculateCompleted(List<LoopChecklist> checklists) {
        return checklists != null ? (int) checklists.stream().filter(LoopChecklist::getCompleted).count() : 0;
    }
}
