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
    @NotBlank private String email;
//    String password;
    private String nickname;
//    String phone;
//    Member.Gender gender;
//    LocalDate birthday;
    private Member.OAuthProvider provider;
    private String providerId;
}
