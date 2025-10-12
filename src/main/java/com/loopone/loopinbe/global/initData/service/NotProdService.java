package com.loopone.loopinbe.global.initData.service;

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
    private List<String> memberEmails = new ArrayList<>();

    // 1) 트랜잭션 내 데이터 생성 메서드
    @Transactional
    public void initDummyDataTransactional() {
        notProdMemberService.createMembers(memberEmails);
    }

    // 2) 트랜잭션 커밋 후 가데이터 정보 출력
    public void initDummyData() {
        long start = System.currentTimeMillis();
        initDummyDataTransactional();
        long end = System.currentTimeMillis();
        long executionTimeMillis = end - start;
        NotProdPrintTestAccount.printTestAccounts(
                memberEmails,
                executionTimeMillis
        );
    }
}
