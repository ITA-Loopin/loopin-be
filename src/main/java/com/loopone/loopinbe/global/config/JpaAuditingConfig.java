package com.loopone.loopinbe.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
@Profile("!test")     // ← 테스트에선 로드 안 함
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.empty(); // 실제 구현이면 적절히
    }
}
