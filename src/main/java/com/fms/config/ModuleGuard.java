package com.fms.config;

import com.fms.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Optional modules: when a module is switched off in Settings, its pages
 * answer "not found", as if they didn't exist.
 */
@Configuration
public class ModuleGuard implements WebMvcConfigurer {

    private final ConfigService config;

    public ModuleGuard(ConfigService config) {
        this.config = config;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                var c = config.get();
                boolean off = (path.startsWith("/cash") && !c.isModCash())
                        || (path.startsWith("/cost-centers") && !c.isModCostCenters());
                if (off) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return false;
                }
                return true;
            }
        }).addPathPatterns("/cash/**", "/cash", "/cost-centers/**", "/cost-centers");
    }
}
