package com.tickethub.security;

import com.tickethub.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our {@link User} entity to Spring Security's {@link UserDetails} contract.
 *
 * <p>Spring Security never touches the entity directly. It works through this wrapper,
 * which means the entity is free to evolve without coupling to the security API.</p>
 *
 * <p>The role is prefixed with {@code ROLE_} because Spring's {@code hasRole("ADMIN")}
 * matcher automatically prepends the prefix before comparing — so the stored authority
 * must be {@code ROLE_ADMIN} to match {@code hasRole("ADMIN")}.</p>
 */
public class AppUserDetails implements UserDetails {

    private final Long   id;
    private final String email;
    private final String passwordHash;
    private final List<GrantedAuthority> authorities;

    public AppUserDetails(User user) {
        this.id           = user.getId();
        this.email        = user.getEmail();
        this.passwordHash = user.getPassword();
        // "ROLE_" prefix is required by Spring Security's hasRole() / hasAuthority() checks
        this.authorities  = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Used by JwtService and BookingController to identify the caller. */
    public Long getId() {
        return id;
    }

    // ---- UserDetails interface ----------------------------------------------

    @Override public String getUsername()               { return email; }
    @Override public String getPassword()               { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    // Account is never locked or expired in this implementation.
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }
}

