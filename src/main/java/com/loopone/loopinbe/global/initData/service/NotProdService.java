package com.loopone.loopinbe.global.initData.service;

import com.loopone.loopinbe.global.initData.loop.service.NotProdLoopService;
import com.loopone.loopinbe.global.initData.member.service.NotProdMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdService {
    private final NotProdMemberService notProdMemberService;
    private final NotProdLoopService notProdLoopService;
    private List<String> memberEmails = new ArrayList<>();

    // 1) 가데이터 생성 메서드
    public void initDummyDataTransactional() {
        notProdMemberService.createMembers(memberEmails);
//        notProdLoopService.createWeekLoops();
//        notProdLoopService.completeScenario_1_1();
//        notProdLoopService.completeScenario_1_2();
        notProdLoopService.createMonthLoops();
        notProdLoopService.completeScenario_2_1();
//        notProdLoopService.completeScenario_2_2();
    }

    // 2) 가데이터 정보 출력
    public void initDummyData() {
        long start = System.currentTimeMillis();
        initDummyDataTransactional();
        long end = System.currentTimeMillis();
        long executionTimeMillis = end - start;
        NotProdPrintService.printTestAccounts(
                memberEmails,
                executionTimeMillis
        );
    }
}
