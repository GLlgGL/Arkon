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

    private static final DateTimeFormatter LAST_LOGIN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserService users,
                                           com.fms.service.AuditService audit) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/admin/**", "/classes/**", "/settings/**", "/reports/**", "/statistics/**",
                        "/stock/**", "/debts/**", "/shelves/**",
                        "/purchase-orders/**", "/cash/**", "/cost-centers/**").hasRole("ADMIN")
                .requestMatchers("/invoices/purchases/**").hasRole("ADMIN")
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
            response.sendRedirect(request.getContextPath() + "/home");
        };
    }
}
