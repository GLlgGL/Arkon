package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.NotFoundException;
import com.fms.repo.AppUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login and password handling.
 *
 * The original compared plain-text passwords in SQL. Imported passwords are
 * stored as "{noop}<old password>"; Spring Security then accepts them once and
 * {@link #updatePassword} immediately re-saves them as BCrypt hashes.
 */
@Service
public class UserService implements UserDetailsService, UserDetailsPasswordService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(u.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER")))
                .build();
    }

    /** Called by Spring Security when a stored hash should be upgraded (e.g. {noop} to {bcrypt}). */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newEncodedPassword) {
        users.findByUsername(user.getUsername()).ifPresent(u -> {
            u.setPassword(newEncodedPassword);
            users.save(u);
        });
        return User.withUserDetails(user).password(newEncodedPassword).build();
    }

    @Transactional(readOnly = true)
    public AppUser get(String username) {
        return users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Records this login and returns the previous one, for the welcome line. */
    @Transactional
    public LocalDateTime recordLogin(String username) {
        AppUser u = get(username);
        LocalDateTime previous = u.getLastLoginAt();
        u.setLastLoginAt(LocalDateTime.now());
        users.save(u);
        return previous;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser u = get(username);
        if (!encoder.matches(currentPassword, u.getPassword())) {
            throw new IllegalArgumentException("The current password is wrong.");
        }
        setPassword(u, newPassword);
    }

    @Transactional
    public void setPassword(AppUser u, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("The new password must be at least 8 characters.");
        }
        u.setPassword(encoder.encode(newPassword));
        u.setLastPasswordChangeAt(LocalDateTime.now());
        users.save(u);
    }
}
