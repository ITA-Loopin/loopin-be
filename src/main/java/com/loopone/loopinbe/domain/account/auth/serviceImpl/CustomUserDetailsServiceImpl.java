package com.loopone.loopinbe.domain.account.auth.serviceImpl;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import com.loopone.loopinbe.domain.account.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        // 비밀번호 보정: null/empty 방지
        String password = member.getPassword();
        if (!org.springframework.util.StringUtils.hasText(password)) {
            password = "{noop}SOCIAL_LOGIN_USER";   // 더미값 (null/empty 금지)
        } else if (!password.startsWith("{")) {
            password = "{noop}" + password;     // 비암호화 평문이 저장되어 있는 특수 케이스라면 명시적으로 noop
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(member.getEmail())
                .password(member.getPassword()) // 패스워드는 보통 사용 안 함 (JWT 기반 인증)
                .authorities("ROLE_USER") // 권한 설정 (추후 DB에서 가져올 수 있음)
                .build();
    }
}
