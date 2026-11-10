package com.bakban9.security;

import com.bakban9.domain.Admin;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminPrincipal implements UserDetails, CredentialsContainer, Serializable {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final boolean active;
    private final Instant lockedUntil;
    private String passwordHash;

    public AdminPrincipal(Admin a) {
        this.id = a.getId();
        this.email = a.getEmail();
        this.name = a.getName();
        this.role = a.getRole();
        this.active = a.isActive();
        this.lockedUntil = a.getLockedUntil();
        this.passwordHash = a.getPasswordHash();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }

    public boolean isOwner() {
        return Admin.OWNER.equals(role);
    }

    public String getRoleLabel() {
        return isOwner() ? "เจ้าของร้าน" : "ผู้แก้ไข";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isEnabled() { return active; }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
