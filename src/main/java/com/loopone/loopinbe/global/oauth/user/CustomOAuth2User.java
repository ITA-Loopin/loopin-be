package com.loopone.loopinbe.global.oauth.user;

import com.loopone.loopinbe.domain.account.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String email;
    private final Member.OAuthProvider provider;
    private final String providerId;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String email,
                            Member.OAuthProvider provider,
                            String providerId) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

    public String getEmail() { return email; }
    public Member.OAuthProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getName() { return providerId; }
}
