package com.loopone.loopinbe.domain.account.member.dto.req;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.service.RegularSignUp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {
    @NotBlank String email;
//    @NotBlank(groups = RegularSignUp.class)
    String password;    // 일반 회원가입 때만 필수
    String nickname;
//    String phone;
//    Member.Gender gender;
//    LocalDate birthday;
    Member.OAuthProvider provider;
    String providerId;
}
