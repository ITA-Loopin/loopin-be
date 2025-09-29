package com.loopone.loopinbe.global.initData.utils;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NotProdUtils {
    private final Random random = new Random();
    private static final String[] SURNAMES = {
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"
    };
    private static final String[] FIRST_NAMES = {
            "민준", "서준", "예준", "도윤", "시우", "하준", "주원", "지후", "지훈", "준서",
            "서연", "서윤", "하은", "지우", "지유", "하린", "수아", "서현", "예은", "채원"
    };
    private static final String[] OLD_FIRST_NAMES = {
            "영희", "철수", "말자", "순자", "명자", "춘자", "옥자", "영자", "숙자", "정자",
            "갑순", "기순", "옥순", "순희", "순복", "용자", "형철", "병수", "춘호", "길동"
    };

    // 이름 생성
    public String generateRandomFullName(boolean old) {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];
        String firstName = old ? OLD_FIRST_NAMES[random.nextInt(OLD_FIRST_NAMES.length)]
                : FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        return surname + firstName;
    }

    // 범위내 랜덤 생년월일 생성
    public LocalDate generateRandomBirthday(int startYear, int endYear) {
        int year = ThreadLocalRandom.current().nextInt(startYear, endYear + 1);
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, YearMonth.of(year, month).lengthOfMonth() + 1);
        return LocalDate.of(year, month, day);
    }

    // 생년월일로 비밀번호 생성
    public String generatePasswordFromBirthday(LocalDate birthday) {
        return String.format("%02d%02d%02d", birthday.getYear() % 100, birthday.getMonthValue(), birthday.getDayOfMonth());
    }

    // 랜덤 전화번호 생성
    public String generateRandomPhone() {
        return String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
    }

    // 선생님 랜덤 ID 생성
    public Long generateAccountId() {
        int accountPrefix = 2000 + random.nextInt(25);
        long accountSuffix = 10000 + random.nextInt(90000);
        return Long.parseLong(accountPrefix + String.valueOf(accountSuffix));
    }

    // 관리자 랜덤 ID 생성
    public long generateRandom9DigitAccountId() {
        return Long.parseLong(String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L)));
    }
}
