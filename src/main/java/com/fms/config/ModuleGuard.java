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
                        || (path.startsWith("/cost-centers") && !c.isModCostCenters())
                        || (path.startsWith("/payroll") && !c.isModPayroll())
                        || (path.startsWith("/assets") && !c.isModAssets())
                        || (path.startsWith("/production") && !c.isModProduction())
                        || (path.startsWith("/taxes") && !c.isModTaxes())
                        || (path.startsWith("/accounting") && !c.isModAccounting())
                        || (path.startsWith("/statements") && !c.isModStatements())
                        || (path.startsWith("/stock-count") && !c.isModStockCount())
                        || (path.startsWith("/reminders") && !c.isModReminders())
                        || (path.startsWith("/budgets") && !c.isModBudgets())
                        || (path.startsWith("/pos") && !c.isModPos())
                        || (path.startsWith("/admin/rates") && !c.isModCurrencies())
                        || (path.startsWith("/reports/fx-differences") && !c.isModCurrencies())
                        || (path.startsWith("/admin/price-lists") && !c.isModPriceLists())
                        || ((path.startsWith("/account/2fa") || path.startsWith("/login/2fa")) && !c.isMod2fa());
                if (off) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return false;
                }
                return true;
            }
        }).addPathPatterns("/cash/**", "/cash", "/cost-centers/**", "/cost-centers",
                "/payroll/**", "/payroll", "/assets/**", "/assets", "/production/**", "/production", "/taxes/**", "/taxes",
                "/accounting/**", "/accounting", "/statements/**", "/stock-count/**", "/stock-count", "/reminders/**", "/reminders",
                "/budgets/**", "/budgets", "/pos/**", "/pos", "/admin/rates/**", "/admin/rates", "/reports/fx-differences", "/admin/price-lists/**", "/admin/price-lists", "/account/2fa/**", "/account/2fa", "/login/2fa");
    }
}
