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
    private final List<Long> studentIds = new ArrayList<>();
    private final List<Long> allStudentIds = new ArrayList<>();
    private final List<String> studentPasswords = new ArrayList<>();
    private final List<String> allStudentPasswords = new ArrayList<>();
    private final List<Long> parentIds = new ArrayList<>();
    private final List<Long> allParentIds = new ArrayList<>();
    private final List<Long> teacherIds = new ArrayList<>();
    private final List<String> teacherPasswords = new ArrayList<>();
    List<Long> adminIdHolder = new ArrayList<>();
    List<String> adminPwHolder = new ArrayList<>();
    private Long adminAccountId;
    private String adminPassword;

    // 1) 트랜잭션 내 데이터 생성 메서드
    @Transactional
    public void initDummyDataTransactional() {
//        notProdMemberService.createStudents(studentIds, studentPasswords, allStudentIds, allStudentPasswords);
//        notProdMemberService.createParents(studentIds, parentIds, allStudentIds, allStudentPasswords, allParentIds);
//        notProdMemberService.followChild(allStudentIds, allParentIds);
//        notProdMemberService.createTeachers(teacherIds, teacherPasswords);
//        notProdMemberService.createAdmin(adminIdHolder, adminPwHolder);
//        this.adminAccountId = adminIdHolder.get(0);
//        this.adminPassword = adminPwHolder.get(0);
    }

    // 2) 트랜잭션 커밋 후 가데이터 정보 출력
    public void initDummyData() {
        long start = System.currentTimeMillis();
        initDummyDataTransactional();
        long end = System.currentTimeMillis();
        long executionTimeMillis = end - start;
        NotProdPrintTestAccount.printTestAccounts(
                studentIds,
                studentPasswords,
                parentIds,
                teacherIds,
                teacherPasswords,
                String.valueOf(adminAccountId),
                adminPassword,
                executionTimeMillis
        );
    }
}
