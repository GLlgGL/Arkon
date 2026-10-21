package com.fms.config;

import com.fms.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Replaces the IsUserLogedIn() method copied into every page, which checked
 * Session["IsUserLogin"] and Session["UserRole"] by hand.
 *
 * Role 1 (Administrator): everything.
 * Role 2 (User): new purchases, sales and offers; their own lists; sale and
 *                offer invoices (view only); their own password.
 *
 * Login uses Spring Security's standard setup: it finds UserService (the
 * UserDetailsService and UserDetailsPasswordService) and the PasswordEncoder
 * bean by itself, including re-hashing imported {noop} passwords on login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.service.ConfigService configService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.AppUserRepository userRepository;

    private static final DateTimeFormatter LAST_LOGIN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserService users,
                                           com.fms.service.AuditService audit) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", "/manifest.webmanifest", "/sw.js", "/offline.html", "/fonts/**").permitAll()
                // Administration stays for administrators. Every other area is checked per user by AccessGuard.
                .requestMatchers("/admin/**", "/classes/**", "/settings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/invoices/*/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/*/*/delete").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccess(users, audit))
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    /** Stores the previous login time for the welcome line, then records this login. */
    private AuthenticationSuccessHandler loginSuccess(UserService users, com.fms.service.AuditService audit) {
        return (request, response, authentication) -> {
            var previous = users.recordLogin(authentication.getName());
            audit.logAs(authentication.getName(), "LOGIN", "User", authentication.getName(), request.getRemoteAddr());
            HttpSession session = request.getSession();
            session.setAttribute("previousLogin", previous == null ? "first login" : previous.format(LAST_LOGIN));
            // Two-step login: ask for the code, or for setting it up when it's required.
            String next = "/home";
            var cfg = configService.get();
            if (cfg.isMod2fa()) {
                var u = userRepository.findByUsername(authentication.getName()).orElse(null);
                if (u != null && u.isTotpEnabled()) {
                    session.setAttribute(TwoFactorGuard.PENDING, Boolean.TRUE);
                    next = "/login/2fa";
                } else if (u != null && cfg.isTfaRequired()) {
                    session.setAttribute(TwoFactorGuard.SETUP, Boolean.TRUE);
                    next = "/account/2fa";
                }
            }
            response.sendRedirect(request.getContextPath() + next);
        };
    }
}
