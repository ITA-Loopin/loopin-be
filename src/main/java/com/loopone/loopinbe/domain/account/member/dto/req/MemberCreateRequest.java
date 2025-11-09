package com.loopone.loopinbe.domain.account.member.dto.req;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;
//    String password;
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
    private String nickname;
//    String phone;
//    Member.Gender gender;
//    LocalDate birthday;
    @NotNull(message = "provider는 필수입니다.")
    private Member.OAuthProvider provider;
    @NotBlank(message = "providerId는 필수입니다.")
    private String providerId;
}
