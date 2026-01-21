package com.example.workflow_management_system.security;

import com.example.workflow_management_system.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
    private Long tenantId;
    private String tenantName;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email, String password, String role, Long tenantId,
            String tenantName, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = Collections
                .singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getTenant() != null ? user.getTenant().getName() : null,
                authorities);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // for now
    }

}
