package com.loopone.loopinbe.domain.loop.loopChecklist.serviceImpl;

import com.loopone.loopinbe.domain.account.auth.currentUser.CurrentUserDto;
import com.loopone.loopinbe.domain.loop.helper.LoopCacheEvictionHelper;
import com.loopone.loopinbe.domain.loop.loop.entity.Loop;
import com.loopone.loopinbe.domain.loop.loop.repository.LoopRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistCreateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.req.LoopChecklistUpdateRequest;
import com.loopone.loopinbe.domain.loop.loopChecklist.dto.res.LoopChecklistResponse;
import com.loopone.loopinbe.domain.loop.loopChecklist.entity.LoopChecklist;
import com.loopone.loopinbe.domain.loop.loopChecklist.repository.LoopChecklistRepository;
import com.loopone.loopinbe.domain.loop.loopChecklist.service.LoopChecklistService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoopChecklistServiceImpl implements LoopChecklistService {
    private final LoopChecklistRepository LoopChecklistRepository;
    private final LoopRepository loopRepository;
    private final CacheManager cacheManager;
    private final LoopCacheEvictionHelper loopCacheEvictionHelper;

    //체크리스트 생성
    @Override
    @Transactional
    public LoopChecklistResponse addLoopChecklist(Long loopId, LoopChecklistCreateRequest loopChecklistCreateRequest, CurrentUserDto currentUser){
        //부모 루프 찾기
        Loop loop = loopRepository.findById(loopId)
                .orElseThrow(() -> new ServiceException(ReturnCode.LOOP_NOT_FOUND));

        //루프의 소유자가 현재 사용자인지 확인
        validateLoopOwner(loop,currentUser);

        //체크리스트 엔티티를 생성
        LoopChecklist checklist = LoopChecklist.builder()
                .content(loopChecklistCreateRequest.content())
                .loop(loop)
                .build();

        LoopChecklist savedChecklist = LoopChecklistRepository.save(checklist);

        // 커밋 후 캐시 무효화
        LocalDate loopDate = loop.getLoopDate();
        YearMonth ym = (loopDate != null) ? YearMonth.from(loopDate) : null;
        loopCacheEvictionHelper.evictAfterCommit(
                currentUser.id(),
                List.of(loop.getId()),
                (loopDate != null) ? List.of(loopDate) : List.of(),
                (ym != null) ? List.of(ym) : List.of()
        );
        return LoopChecklistResponse.builder()
                .id(savedChecklist.getId())
                .content(savedChecklist.getContent())
                .completed(savedChecklist.getCompleted())
                .build();
    }

    //체크리스트 수정
    @Override
    @Transactional
    public void updateLoopChecklist(Long checkListId, LoopChecklistUpdateRequest loopChecklistUpdateRequest, CurrentUserDto currentUser){
        //수정할 체크리스트를 찾기
        LoopChecklist loopChecklist = LoopChecklistRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));

        //체크리스트의 소유자가 현재 사용자인지 확인
        validateLoopChecklistOwner(loopChecklist, currentUser);

        //체크리스트 수정
        if (loopChecklistUpdateRequest.content() != null) {
            loopChecklist.setContent(loopChecklistUpdateRequest.content());
        }
        if (loopChecklistUpdateRequest.completed() != null) {
            loopChecklist.setCompleted(loopChecklistUpdateRequest.completed());
        }

        // 커밋 후 캐시 무효화
        Loop loop = loopChecklist.getLoop();
        LocalDate loopDate = loop.getLoopDate();
        YearMonth ym = (loopDate != null) ? YearMonth.from(loopDate) : null;

        loopCacheEvictionHelper.evictAfterCommit(
                currentUser.id(),
                List.of(loop.getId()),
                (loopDate != null) ? List.of(loopDate) : List.of(),
                (ym != null) ? List.of(ym) : List.of()
        );
    }

    //체크리스트 삭제
    @Override
    @Transactional
    public void deleteLoopChecklist(Long checkListId, CurrentUserDto currentUser) {
        //삭제할 체크리스트를 찾기
        LoopChecklist loopChecklist = LoopChecklistRepository.findById(checkListId)
                .orElseThrow(() -> new ServiceException(ReturnCode.CHECK_LIST_NOT_FOUND));

        //체크리스트의 소유자가 현재 사용자인지 확인
        validateLoopChecklistOwner(loopChecklist, currentUser);

        // delete 전에 캐시 무효화에 필요한 값 확보
        Loop loop = loopChecklist.getLoop();
        Long loopId = loop.getId();
        LocalDate loopDate = loop.getLoopDate();
        YearMonth ym = (loopDate != null) ? YearMonth.from(loopDate) : null;
        LoopChecklistRepository.delete(loopChecklist);

        // 커밋 후 캐시 무효화
        loopCacheEvictionHelper.evictAfterCommit(
                currentUser.id(),
                List.of(loopId),
                (loopDate != null) ? List.of(loopDate) : List.of(),
                (ym != null) ? List.of(ym) : List.of()
        );
    }

    //루프 내의 체크리스트 전체 삭제
    @Override
    @Transactional
    public void deleteAllChecklist(Long loopId){
        List<LoopChecklist> loopChecklists = LoopChecklistRepository.findByLoopId(loopId);
        LoopChecklistRepository.deleteAll(loopChecklists);
    }

    // ========== 검증 메서드 ==========
    //체크리스트 사용자 검증
    public static void validateLoopChecklistOwner(LoopChecklist loopChecklist, CurrentUserDto currentUser) {
        if (!loopChecklist.getLoop().getMember().getId().equals(currentUser.id())) {
            throw new ServiceException(ReturnCode.CHECKLIST_ACCESS_DENIED);
        }
    }

    //루프 사용자 검증
    public static void validateLoopOwner(Loop loop, CurrentUserDto currentUser) {
        if (!loop.getMember().getId().equals(currentUser.id())) {
            throw new ServiceException(ReturnCode.LOOP_ACCESS_DENIED);
        }
    }
}
