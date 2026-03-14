package com.smartcommerce.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class JWTAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final String token;

    private JWTAuthenticationToken(UserDetails principal,
                                   String token,
                                   Collection<? extends GrantedAuthority> authorities,
                                   boolean authenticated) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        super.setAuthenticated(authenticated);
    }

    public static JWTAuthenticationToken authenticated(UserDetails principal,
                                                       String token,
                                                       Collection<? extends GrantedAuthority> authorities) {
        return new JWTAuthenticationToken(principal, token, authorities, true);
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot mark JWTAuthenticationToken authenticated via setter");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
