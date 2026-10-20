package com.fms.config;

import com.fms.domain.Area;
import com.fms.repo.AppUserRepository;
import com.fms.service.AccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enforces access per area on every request: viewing a page needs at least
 * View access; anything that changes data (POST) needs Full access.
 */
@Configuration
public class AccessGuard implements WebMvcConfigurer {

    private final AccessService access;
    private final AppUserRepository users;

    public AccessGuard(AccessService access, AppUserRepository users) {
        this.access = access;
        this.users = users;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                Area area = Area.forPath(path);
                if (area == null || request.getUserPrincipal() == null) {
                    return true;
                }
                var user = users.findByUsername(request.getUserPrincipal().getName()).orElse(null);
                if (user == null) {
                    return true;
                }
                boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
                Area.Level need = read ? Area.Level.VIEW : Area.Level.FULL;
                if (!access.level(user, area).atLeast(need)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
                return true;
            }
        });
    }
}
