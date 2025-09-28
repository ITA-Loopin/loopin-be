package com.loopone.loopinbe.global.initData.utils;

import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import com.iEdu.global.common.enums.Semester;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    // 범위내 랜덤 숫자 생성
    public List<Integer> getRandomUniqueNumbers(int start, int end, int count) {
        List<Integer> numbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        Collections.shuffle(numbers);
        return numbers.subList(0, count);
    }

    // 점수 데이터 생성
    public double generateScore() {
        double score;
        double rand = random.nextDouble();
        if (rand < 0.6) {
            score = 70 + random.nextDouble() * 20;
        } else if (rand < 0.8) {
            score = 60 + random.nextDouble() * 10;
        } else {
            score = 91 + random.nextDouble() * 9;
        }
        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    // 학기별 수업일 계산
    public List<LocalDate> getSchoolDaysForSemester(int baseYear, Semester semester) {
        LocalDate start = semester == Semester.FIRST_SEMESTER
                ? LocalDate.of(baseYear, 3, 2)
                : LocalDate.of(baseYear, 9, 1);
        LocalDate end = semester == Semester.FIRST_SEMESTER
                ? LocalDate.of(baseYear, 7, 15)
                : LocalDate.of(baseYear, 12, 31);
        List<LocalDate> schoolDays = new ArrayList<>();
        LocalDate date = start;

        while (!date.isAfter(end)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                schoolDays.add(date);
            }
            date = date.plusDays(1);
        }
        return schoolDays;
    }

    // 출석 상태 랜덤 생성
    public PeriodAttendance.State generateRandomState() {
        double rand = Math.random();
        if (rand < 0.95) return PeriodAttendance.State.출석;
        int pick = (int) (Math.random() * 3);
        return switch (pick) {
            case 0 -> PeriodAttendance.State.결석;
            case 1 -> PeriodAttendance.State.지각;
            default -> PeriodAttendance.State.조퇴;
        };
    }
}
