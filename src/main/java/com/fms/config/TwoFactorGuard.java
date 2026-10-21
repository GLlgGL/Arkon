package com.fms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Two-step login: after the password, nothing opens until the code is
 * entered (PENDING) or, when two-step is required, until it's set up (SETUP).
 */
@Configuration
public class TwoFactorGuard implements WebMvcConfigurer {

    public static final String PENDING = "tfaPending";
    public static final String SETUP = "tfaSetup";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                var session = request.getSession(false);
                if (session == null) return true;
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/") || path.startsWith("/fonts/")
                        || path.equals("/favicon.ico") || path.startsWith("/error") || path.startsWith("/logout")) {
                    return true;
                }
                if (session.getAttribute(PENDING) != null && !path.startsWith("/login/2fa")) {
                    response.sendRedirect(request.getContextPath() + "/login/2fa");
                    return false;
                }
                if (session.getAttribute(SETUP) != null && !path.startsWith("/account/2fa")) {
                    response.sendRedirect(request.getContextPath() + "/account/2fa");
                    return false;
                }
                return true;
            }
        });
    }
}
