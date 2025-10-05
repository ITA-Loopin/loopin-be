package com.loopone.loopinbe.global.initData.service;

import java.util.List;

public class NotProdPrintTestAccount {

    // 가데이터 정보 출력
    public static void printTestAccounts(
            List<String> memberEmails,
            List<String> memberPasswords,
            long executionTimeMillis
    ) {
        System.out.println("\n\n--- 회원 더미데이터 계정 정보 ---");
        for (int i = 0; i < memberEmails.size(); i++) {
            System.out.println("\n--- 회원 " + (i + 1) + " ---");
            System.out.println("이메일: " + memberEmails.get(i));
            System.out.println("비밀번호: " + memberPasswords.get(i));
        }

        // 실행 시간 출력 (시/분/초 단위로)
        long seconds = executionTimeMillis / 1000 % 60;
        long minutes = executionTimeMillis / (1000 * 60) % 60;
        long hours = executionTimeMillis / (1000 * 60 * 60);
        System.out.printf("\n\n=== 전체 더미 데이터 생성 시간: %02d시간 %02d분 %02d초 ===%n", hours, minutes, seconds);
    }
}
