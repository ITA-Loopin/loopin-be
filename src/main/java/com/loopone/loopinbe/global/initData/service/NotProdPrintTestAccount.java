package com.loopone.loopinbe.global.initData.service;

import java.util.List;

public class NotProdPrintTestAccount {

    // 가데이터 정보 출력
    public static void printTestAccounts(
            List<Long> studentIds,
            List<String> studentPasswords,
            List<Long> parentIds,
            List<Long> teacherIds,
            List<String> teacherPasswords,
            String adminAccountId,
            String adminPassword,
            long executionTimeMillis
    ) {
        System.out.println("\n\n--- 학생/학부모 계정 정보 (1~3학년 1반 1번 학생/학부모) ---");
        for (int i = 0; i < studentIds.size(); i++) {
            System.out.println("\n--- " + (i + 1) + "학년 1반 1번 학생 ---");
            System.out.println("계정 ID: " + studentIds.get(i));
            System.out.println("학부모 계정 ID: " + parentIds.get(i));
            System.out.println("비밀번호: " + studentPasswords.get(i));
        }

        System.out.println("\n\n--- 선생님 계정 정보 (1~3학년 1반 선생님) ---");
        for (int i = 0; i < teacherIds.size(); i++) {
            System.out.println("\n--- " + (i + 1) + "학년 1반 선생님 ---");
            System.out.println("계정 ID: " + teacherIds.get(i));
            System.out.println("비밀번호: " + teacherPasswords.get(i));
        }

        System.out.println("\n\n--- 관리자 계정 정보 ---");
        System.out.println("계정 ID: " + adminAccountId);
        System.out.println("비밀번호: " + adminPassword);

        // 실행 시간 출력 (시/분/초 단위로)
        long seconds = executionTimeMillis / 1000 % 60;
        long minutes = executionTimeMillis / (1000 * 60) % 60;
        long hours = executionTimeMillis / (1000 * 60 * 60);
        System.out.printf("\n\n=== 전체 더미 데이터 생성 시간: %02d시간 %02d분 %02d초 ===%n", hours, minutes, seconds);
    }
}
