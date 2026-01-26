package com.loopone.loopinbe.domain.account.auth.service;

import com.loopone.loopinbe.domain.account.auth.serviceImpl.CustomUserDetailsServiceImpl;
import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.auth.dummy-password={noop}dummy-password"
})
@Import({
        TestContainersConfig.class,
        CustomUserDetailsServiceImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomUserDetailsServiceTest {
    // ===== Real Repositories =====
    @Autowired MemberRepository memberRepository;

    // ===== SUT =====
    @Autowired CustomUserDetailsServiceImpl customUserDetailsService;

    @AfterEach
    void cleanup() {
        memberRepository.deleteAll();
    }

    // ===== Helpers =====
    private Member persistMember(String email, String password) {
        Member m = Member.builder()
                .email(email)
                .password(password)
                .nickname("nick")
                .profileImageUrl(null)
                .state(Member.State.NORMAL)
                .role(Member.MemberRole.ROLE_USER)
                .oAuthProvider(Member.OAuthProvider.GOOGLE)
                .providerId("pid")
                .build();
        return memberRepository.saveAndFlush(m);
    }

    // =========================================================
    // loadUserByUsername
    // =========================================================
    @Nested
    class LoadUserByUsername {

        @Test
        @DisplayName("실패: 멤버가 없으면 UsernameNotFoundException")
        void userNotFound() {
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("no@loop.in"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("성공: password가 null/blank면 dummy-password 사용")
        void nullOrBlankPasswordUsesDummy() {
            String email = "a@loop.in";
            persistMember(email, "  ");

            UserDetails user = customUserDetailsService.loadUserByUsername(email);

            assertThat(user.getUsername()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo("{noop}dummy-password");
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .contains("ROLE_USER");
        }

        @Test
        @DisplayName("성공: password가 인코딩 prefix가 없으면 {noop} 추가")
        void addNoopPrefixWhenMissing() {
            String email = "plain@loop.in";
            persistMember(email, "plain-pass");

            UserDetails user = customUserDetailsService.loadUserByUsername(email);

            assertThat(user.getPassword()).isEqualTo("{noop}plain-pass");
        }

        @Test
        @DisplayName("성공: password가 인코딩 prefix를 갖고 있으면 유지")
        void keepEncodedPassword() {
            String email = "encoded@loop.in";
            persistMember(email, "{bcrypt}hash");

            UserDetails user = customUserDetailsService.loadUserByUsername(email);

            assertThat(user.getPassword()).isEqualTo("{bcrypt}hash");
        }
    }
}
