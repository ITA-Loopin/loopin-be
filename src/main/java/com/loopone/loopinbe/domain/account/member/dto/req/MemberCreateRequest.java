package com.loopone.loopinbe.domain.account.member.dto.req;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {
    @NotBlank
    private String email;
//    String password;
    @NotBlank
    @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
    private String nickname;
//    String phone;
//    Member.Gender gender;
//    LocalDate birthday;
    @NotBlank
    private Member.OAuthProvider provider;
    @NotBlank
    private String providerId;
}
