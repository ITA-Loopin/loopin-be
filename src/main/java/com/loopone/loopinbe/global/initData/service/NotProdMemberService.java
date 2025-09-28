package com.loopone.loopinbe.global.initData.service;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.account.member.dto.req.FollowRequest;
import com.iEdu.domain.account.member.dto.req.MemberRequest;
import com.iEdu.domain.account.member.dto.req.ParentSignUpRequest;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.mapper.MemberMapper;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.initData.utils.NotProdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdMemberService {
    private final AdminService adminService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final NotProdUtils notProdUtils;
    private final MemberMapper memberMapper;
    private final Random random = new Random();

    // 학생 가데이터 생성
    public void createStudents(List<Long> studentIds, List<String> studentPasswords, List<Long> allStudentIds, List<String> allStudentPasswords) {
        int emailCounter = 1;
        Map<Integer, Set<Long>> gradeUsedSuffixMap = new HashMap<>();
        for (int grade = 1; grade <= 3; grade++) {
            int birthYear = 2010 - grade;
            String gradePrefix = String.valueOf(2026 - grade);
            gradeUsedSuffixMap.put(grade, new HashSet<>());

            for (int classId = 1; classId <= 7; classId++) {
                Set<String> usedNames = new HashSet<>();

                for (int number = 1; number <= 25; number++) {
                    String name = notProdUtils.generateRandomFullName(false);
                    usedNames.add(name);

                    LocalDate birthday = notProdUtils.generateRandomBirthday(birthYear, birthYear);
                    String password = notProdUtils.generatePasswordFromBirthday(birthday);

                    long studentSuffix;
                    do {
                        studentSuffix = 10000 + random.nextInt(90000); // 5자리
                    } while (gradeUsedSuffixMap.get(grade).contains(studentSuffix));
                    gradeUsedSuffixMap.get(grade).add(studentSuffix);
                    long accountId = Long.parseLong(gradePrefix + studentSuffix);
                    allStudentIds.add(accountId);       // 모든 학생 ID 추가
                    allStudentPasswords.add(password);  // 모든 학생 Password 추가
                    String email = "student" + (emailCounter++) + "@school.com";
                    String phone = notProdUtils.generateRandomPhone();

                    // 1~3학년 1반 1번 학생의 정보 저장
                    if (grade <= 3 && classId == 1 && number == 1) {
                        studentIds.add(accountId);
                        studentPasswords.add(password);
                    }
                    MemberRequest student = MemberRequest.builder()
                            .accountId(accountId)
                            .password(password)
                            .name(name)
                            .phone(phone)
                            .email(email)
                            .birthday(birthday)
                            .schoolName("송도고등학교")
                            .year(grade)
                            .classId(classId)
                            .number(number)
                            .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                            .role(Member.MemberRole.ROLE_STUDENT)
                            .state(Member.State.NORMAL)
                            .build();
                    adminService.sudoSignup(student);
                }
            }
        }
        System.out.println("-- 총 525명의 학생 가데이터 생성 완료! --");
    }

    // 학부모 가데이터 생성
    public void createParents(List<Long> studentIds, List<Long> parentIds, List<Long> allStudentIds, List<String> allStudentPasswords, List<Long> allParentIds) {
        for (int i = 0; i < allStudentIds.size(); i++) {
            Long studentAccountId = allStudentIds.get(i);
            String studentPassword = allStudentPasswords.get(i);
            String name = notProdUtils.generateRandomFullName(true);
            int randomTwoDigits = random.nextInt(90) + 10;
            Long parentAccountId = Long.parseLong(studentAccountId + String.valueOf(randomTwoDigits));

            ParentSignUpRequest parentSignUpRequest = ParentSignUpRequest.builder()
                    .accountId(parentAccountId)
                    .password(studentPassword)
                    .name(name)
                    .phone(notProdUtils.generateRandomPhone())
                    .email("parent_" + (i+1) + "@school.com")
                    .birthday(notProdUtils.generateRandomBirthday(1970, 1995))
                    .schoolName("송도고등학교")
                    .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                    .build();
            memberService.signup(parentSignUpRequest);
            allParentIds.add(parentAccountId);

            // 1학년 1반 1번~3학년 1반 1번 학생 학부모만 별도 저장
            if (studentAccountId.equals(studentIds.get(0)) ||
                    studentAccountId.equals(studentIds.get(1)) ||
                    studentAccountId.equals(studentIds.get(2))) {
                parentIds.add(parentAccountId);
            }
        }
        System.out.println("-- 총 525명의 학부모 계정 생성 완료! --");
    }

    // 학생/학부모 팔로우
    public void followChild(List<Long> allStudentIds, List<Long> allParentIds) {
        for (int i = 0; i < allStudentIds.size(); i++) {
            Long studentId = allStudentIds.get(i);
            Long parentId = allParentIds.get(i);
            Optional<Member> optionalStudent = memberRepository.findByAccountId(studentId);
            Optional<Member> optionalParent = memberRepository.findByAccountId(parentId);
            if (optionalStudent.isEmpty() || optionalParent.isEmpty()) {
                log.warn("학생 또는 학부모 정보가 없습니다. studentId: {}, parentId: {}", studentId, parentId);
                continue;
            }

            Member student = optionalStudent.get();
            Member parent = optionalParent.get();
            CurrentUserDto parentLogin = memberMapper.toLoginUserDto(parent);
            CurrentUserDto studentLogin = memberMapper.toLoginUserDto(student);

            FollowRequest followRequest = new FollowRequest();
            followRequest.setName(student.getName());
            followRequest.setYear(student.getYear());
            followRequest.setClassId(student.getClassId());
            followRequest.setNumber(student.getNumber());
            followRequest.setBirthday(student.getBirthday());
            try {
                memberService.followReq(followRequest, parentLogin);
                memberService.acceptFollowReq(parent.getId(), studentLogin);
            } catch (Exception e) {
                log.error("팔로우 처리 중 예외 발생 - studentId: {}, parentId: {}", studentId, parentId, e);
            }
        }
        log.info("-- 총 {} 쌍의 학부모-자녀 팔로우 생성 완료! --", allStudentIds.size());
    }

    // 선생님 가데이터 생성
    public void createTeachers(List<Long> teacherIds, List<String> teacherPasswords) {
        List<Member.Subject> subjectList = List.of(Member.Subject.values());
        int teacherEmailCount = 1;

        for (int year = 1; year <= 3; year++) {
            List<Integer> classIds = notProdUtils.getRandomUniqueNumbers(1, 7, 7);
            List<Member.Subject> shuffleSubjectList = new ArrayList<>(subjectList);
            Collections.shuffle(shuffleSubjectList);

            for (int i = 0; i < 17; i++) {
                String name = notProdUtils.generateRandomFullName(true);
                LocalDate birthday = notProdUtils.generateRandomBirthday(1970, 1995);
                String password = notProdUtils.generatePasswordFromBirthday(birthday);
                Long accountId = notProdUtils.generateAccountId();
                String phone = notProdUtils.generateRandomPhone();
                String email = "teacher" + (teacherEmailCount++) + "@school.com";

                MemberRequest teacher = MemberRequest.builder()
                        .accountId(accountId)
                        .password(password)
                        .name(name)
                        .phone(phone)
                        .email(email)
                        .birthday(birthday)
                        .schoolName("송도고등학교")
                        .year(year)
                        .subject(subjectList.get(i))
                        .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                        .role(Member.MemberRole.ROLE_TEACHER)
                        .state(Member.State.NORMAL)
                        .build();
                if (i < 7) {
                    teacher.setClassId(classIds.get(i));
                }

                // 1~3학년 1반 선생님의 정보 저장
                if (year <= 3 && i < 7 && classIds.get(i) == 1) {
                    teacherIds.add(accountId);
                    teacherPasswords.add(password);
                }
                adminService.sudoSignup(teacher);
            }
        }
        System.out.println("-- 테스트용 교사 51명 생성 완료! --");
    }

    // 관리자 가데이터 생성
    public void createAdmin(List<Long> adminAccountIdHolder, List<String> adminPasswordHolder) {
        Long accountId = notProdUtils.generateRandom9DigitAccountId();
        String password = "iEdu77";
        String phone = notProdUtils.generateRandomPhone();

        MemberRequest adminForm = MemberRequest.builder()
                .accountId(accountId)
                .password(password)
                .name("김송도")
                .phone(phone)
                .email("admin1@school.com")
                .birthday(notProdUtils.generateRandomBirthday(1970, 1995))
                .schoolName("송도고등학교")
                .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                .role(Member.MemberRole.ROLE_ADMIN)
                .state(Member.State.NORMAL)
                .build();
        adminService.sudoSignup(adminForm);
        adminAccountIdHolder.add(accountId);
        adminPasswordHolder.add(password);
        System.out.println("-- 관리자 1명 생성 완료! --");
    }
}
