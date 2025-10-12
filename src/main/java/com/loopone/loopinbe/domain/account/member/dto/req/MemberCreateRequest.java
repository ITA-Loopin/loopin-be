package com.loopone.loopinbe.domain.account.member.dto.req;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {
    @NotBlank String email;
    String password;
    String nickname;
//    String phone;
//    Member.Gender gender;
//    LocalDate birthday;
    Member.OAuthProvider provider;
    String providerId;
}
