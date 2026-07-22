package com.school.canteen.security;

import com.school.canteen.entity.User;
import com.school.canteen.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts our {@link User} entity to Spring Security's {@link UserDetails} contract, which
 * is the shape Spring understands for an authenticated principal. The role becomes a
 * "ROLE_XXX" authority, which is what hasRole('XXX') checks against.
 */
public class AppUserDetails implements UserDetails {

    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        // We authenticate by email, so email is the "username" Spring keys on.
        return user.getEmail();
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
        // A disabled/pending/rejected account is treated as not-enabled, so even a
        // still-valid token stops working the moment an admin changes their status.
        return user.getStatus() == UserStatus.APPROVED;
    }
}
