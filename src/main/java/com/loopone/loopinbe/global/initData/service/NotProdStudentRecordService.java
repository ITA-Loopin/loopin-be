package com.loopone.loopinbe.global.initData.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.mapper.MemberMapper;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceRequest;
import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackRequest;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeRequest;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyRequest;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.initData.utils.NotProdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdStudentRecordService {
    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final FeedbackService feedbackService;
    private final CounselService counselService;
    private final SpecialtyService specialtyService;
    private final MemberRepository memberRepository;
    private final NotProdUtils notProdUtils;
    private final MemberMapper memberMapper;

    // 성적 가데이터 생성
    public void createGradeData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member.Subject> subjectList = List.of(Member.Subject.values());
        for (int studentGrade = 1; studentGrade <= 3; studentGrade++) {
            int startId = (studentGrade - 1) * 175 + 1;
            int endId = studentGrade * 175;

            for (int studentId = startId; studentId <= endId; studentId++) {
                for (int targetGrade = 1; targetGrade <= studentGrade; targetGrade++) {
                    int finalGrade = targetGrade;
                    List<Member> teachersForGrade = teacherList.stream()
                            .filter(t -> t.getYear() == finalGrade)
                            .collect(Collectors.toList());

                    for (int subjectIdx = 0; subjectIdx < subjectList.size(); subjectIdx++) {
                        Member teacher = teachersForGrade.get(subjectIdx);
                        CurrentUserDto loginUser = memberMapper.toLoginUserDto(teacher);

                        for (Semester semester : Semester.values()) {
                            if (semester == Semester.ALL) continue; // ALL 제외
                            if (finalGrade == studentGrade && semester == Semester.SECOND_SEMESTER) continue;
                            GradeRequest gradeRequest = GradeRequest.builder()
                                    .year(finalGrade)
                                    .semester(semester)
                                    .score(notProdUtils.generateScore())
                                    .build();
                            gradeService.createGrade((long) studentId, gradeRequest, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 총 2,100개의 성적 데이터 생성 완료! --");
    }

    // 출결 가데이터 생성
    public void createAttendanceData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member> studentList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_STUDENT)
                .toList();
        for (Member teacher : teacherList) {
            Integer teacherYear = teacher.getYear();
            Integer teacherClassId = teacher.getClassId();

            // 본인 반 학생만 필터링
            List<Member> students = studentList.stream()
                    .filter(s -> Objects.equals(s.getYear(), teacherYear) &&
                            Objects.equals(s.getClassId(), teacherClassId))
                    .toList();
            CurrentUserDto loginUser = memberMapper.toLoginUserDto(teacher);

            for (int targetYear = 1; targetYear <= teacherYear; targetYear++) {
                int baseYear = 2025 - (teacherYear - targetYear);  // 출결 대상 학년이 있었던 실제 연도
                LocalDate today = LocalDate.now();
                for (Semester semester : Semester.values()) {
                    if (semester == Semester.ALL) continue; // ALL 제외
                    if (targetYear == teacherYear && semester != Semester.FIRST_SEMESTER) continue;
                    List<LocalDate> schoolDays = notProdUtils.getSchoolDaysForSemester(baseYear, semester);

                    for (Member student : students) {
                        // 출결 생성 시 년도는 targetYear(과거 학년)으로 세팅
                        for (LocalDate date : schoolDays) {
                            // 현재 학년 1학기일 때, 오늘 이후 날짜는 출결 생성 안 함
                            if (targetYear == teacherYear && semester == Semester.FIRST_SEMESTER && date.isAfter(today)) {
                                continue;
                            }
                            List<PeriodAttendance> periodAttendances = new ArrayList<>();
                            for (PeriodAttendance.Period period : PeriodAttendance.Period.values()) {
                                PeriodAttendance.State state = notProdUtils.generateRandomState();
                                periodAttendances.add(
                                        PeriodAttendance.builder()
                                                .period(period)
                                                .state(state)
                                                .build()
                                );
                            }
                            AttendanceRequest form = AttendanceRequest.builder()
                                    .year(targetYear)  // 출결은 과거 학년 단위로 생성
                                    .semester(semester)
                                    .date(date)
                                    .periodAttendances(periodAttendances)
                                    .build();
                            attendanceService.createAttendance(student.getId(), form, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 약 178,500개의 출결 데이터 생성 완료! --");
    }

    // 피드백 가데이터 생성
    public void createFeedbackData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member> studentList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_STUDENT)
                .toList();
        for (Member teacher : teacherList) {
            Integer teacherYear = teacher.getYear();
            Integer teacherClassId = teacher.getClassId();

            // 본인 반 학생만 필터링
            List<Member> students = studentList.stream()
                    .filter(s -> Objects.equals(s.getYear(), teacherYear) &&
                            Objects.equals(s.getClassId(), teacherClassId))
                    .toList();
            CurrentUserDto loginUser = memberMapper.toLoginUserDto(teacher);
            for (int targetYear = 1; targetYear <= teacherYear; targetYear++) {
                int baseYear = 2025 - (teacherYear - targetYear);

                for (Semester semester : Semester.values()) {
                    if (semester == Semester.ALL) continue;
                    if (targetYear == teacherYear && semester != Semester.FIRST_SEMESTER) continue;
                    List<LocalDate> schoolDays = notProdUtils.getSchoolDaysForSemester(baseYear, semester);
                    if (schoolDays.size() < 4) continue; // 예외 처리
                    LocalDate gradeDate = schoolDays.get(0);
                    LocalDate attitudeDate = schoolDays.get(1);
                    LocalDate attendanceDate = schoolDays.get(2);
                    LocalDate behaviorDate = schoolDays.get(3);

                    for (Member student : students) {
                        feedbackService.createFeedback(student.getId(),
                                FeedbackRequest.builder()
                                        .year(targetYear)
                                        .semester(semester)
                                        .date(gradeDate)
                                        .category(FeedbackCategory.성적)
                                        .content("학기 초에 비해 꾸준한 노력으로 성적이 향상되고 있습니다. 학습 태도를 유지하면 좋을 것 같습니다.")
                                        .visibleToStudent(true)
                                        .visibleToParent(true)
                                        .build(),
                                loginUser);
                        feedbackService.createFeedback(student.getId(),
                                FeedbackRequest.builder()
                                        .year(targetYear)
                                        .semester(semester)
                                        .date(attitudeDate)
                                        .category(FeedbackCategory.태도)
                                        .content("항상 성실한 자세로 수업에 임하며, 발표나 질문에도 적극적인 모습이 인상적입니다.")
                                        .visibleToStudent(true)
                                        .visibleToParent(false)
                                        .build(),
                                loginUser);
                        feedbackService.createFeedback(student.getId(),
                                FeedbackRequest.builder()
                                        .year(targetYear)
                                        .semester(semester)
                                        .date(attendanceDate)
                                        .category(FeedbackCategory.출결)
                                        .content("출결이 매우 우수하며, 지각 없이 항상 제시간에 등교하는 모습이 모범적입니다.")
                                        .visibleToStudent(false)
                                        .visibleToParent(true)
                                        .build(),
                                loginUser);
                        feedbackService.createFeedback(student.getId(),
                                FeedbackRequest.builder()
                                        .year(targetYear)
                                        .semester(semester)
                                        .date(behaviorDate)
                                        .category(FeedbackCategory.행동)
                                        .content("친구들과의 관계가 원만하며, 배려심이 깊어 또래 친구들에게 좋은 영향을 주고 있습니다.")
                                        .visibleToStudent(false)
                                        .visibleToParent(false)
                                        .build(),
                                loginUser);
                    }
                }
            }
        }
        System.out.println("-- 총 8,400개의 피드백 데이터 생성 완료! --");
    }

    // 상담 가데이터 생성
    public void createCounselData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member> studentList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_STUDENT)
                .toList();
        String[] contents = new String[]{
                "최근 수학과 과학 과목에서 성적이 하락한 원인에 대해 부족한 예습·복습 시간을 지적함.",
                "아직 진로를 결정하지 못해 고민하고 있으며, 최근 진로 체험 활동에 관심을 가지기 시작함."
        };
        for (Member teacher : teacherList) {
            Integer teacherYear = teacher.getYear();
            Integer teacherClassId = teacher.getClassId();

            // 본인 반 학생만 필터링
            List<Member> students = studentList.stream()
                    .filter(s -> Objects.equals(s.getYear(), teacherYear) &&
                            Objects.equals(s.getClassId(), teacherClassId))
                    .toList();
            CurrentUserDto loginUser = memberMapper.toLoginUserDto(teacher);
            for (int targetYear = 1; targetYear <= teacherYear; targetYear++) {
                int baseYear = 2025 - (teacherYear - targetYear);

                for (Semester semester : Semester.values()) {
                    if (semester == Semester.ALL) continue;
                    if (targetYear == teacherYear && semester != Semester.FIRST_SEMESTER) continue;
                    List<LocalDate> schoolDays = notProdUtils.getSchoolDaysForSemester(baseYear, semester);
                    if (schoolDays.size() < 2) continue;

                    LocalDate firstDate = schoolDays.get(0);
                    LocalDate secondDate = schoolDays.get(1);
                    for (Member student : students) {
                        for (int i = 0; i < 2; i++) {
                            LocalDate date = (i == 0) ? firstDate : secondDate;
                            LocalDate nextCounselDate = date.plusDays(7);
                            CounselRequest form = new CounselRequest();
                            form.setYear(targetYear);
                            form.setSemester(semester);
                            form.setDate(date);
                            form.setNextCounselDate(nextCounselDate);
                            form.setContent(contents[i]);
                            counselService.createCounsel(student.getId(), form, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 총 4,200개의 상담 데이터 생성 완료! --");
    }

    // 특기사항 가데이터 생성
    public void createSpecialtyData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member> studentList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_STUDENT)
                .toList();
        String[] contents = new String[]{
                "조용하고 차분한 성격으로 학업에 대한 집중력이 뛰어나며, 특히 과학 및 정보 과목에서 뛰어난 문제 해결 능력을 보임.",
                "자기 주도적인 학습 태도가 뛰어나며, 학습 계획을 스스로 세우고 실천하는 데에 있어 꾸준한 모습을 보임."
        };
        for (Member teacher : teacherList) {
            Integer teacherYear = teacher.getYear();
            Integer teacherClassId = teacher.getClassId();

            // 본인 반 학생만 필터링
            List<Member> students = studentList.stream()
                    .filter(s -> Objects.equals(s.getYear(), teacherYear) &&
                            Objects.equals(s.getClassId(), teacherClassId))
                    .toList();
            CurrentUserDto loginUser = memberMapper.toLoginUserDto(teacher);
            for (int targetYear = 1; targetYear <= teacherYear; targetYear++) {
                int baseYear = 2025 - (teacherYear - targetYear);

                for (Semester semester : Semester.values()) {
                    if (semester == Semester.ALL) continue;
                    if (targetYear == teacherYear && semester != Semester.FIRST_SEMESTER) continue;
                    List<LocalDate> schoolDays = notProdUtils.getSchoolDaysForSemester(baseYear, semester);
                    if (schoolDays.size() < 2) continue;

                    LocalDate firstDate = schoolDays.get(0);
                    LocalDate secondDate = schoolDays.get(1);
                    for (Member student : students) {
                        for (int i = 0; i < 2; i++) {
                            LocalDate date = (i == 0) ? firstDate : secondDate;
                            SpecialtyRequest form = new SpecialtyRequest();
                            form.setYear(targetYear);
                            form.setSemester(semester);
                            form.setDate(date);
                            form.setContent(contents[i]);
                            specialtyService.createSpecialty(student.getId(), form, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 총 4,200개의 특기사항 데이터 생성 완료! --");
    }
}
