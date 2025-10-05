package com.loopone.loopinbe.global.initData.service;

import com.loopone.loopinbe.domain.account.member.dto.req.MemberCreateRequest;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.domain.account.member.service.MemberService;
import com.loopone.loopinbe.global.exception.ReturnCode;
import com.loopone.loopinbe.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdMemberService {
    private final MemberService memberService;

    // 유저 1 ~ 5 생성
    public void createMembers(List<String> memberEmails, List<String> memberPasswords) {
        List<String> nicknames = List.of("seoul_gangnam", "incheon_songdo", "gangneung_beach", "busan_haeundae", "jeju_seaside");
        for (int i = 0; i < nicknames.size(); i++){
            MemberCreateRequest memberCreateRequest = MemberCreateRequest.builder()
                    .nickname(nicknames.get(i))
                    .email("user" + (i + 1) + "@example.com")
                    .password("1234")
                    .build();
            Member member = memberService.regularSignUp(memberCreateRequest);
            memberEmails.add(member.getEmail());
            memberPasswords.add("1234");
        }
    }
}
