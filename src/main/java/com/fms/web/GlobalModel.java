package com.fms.web;

import com.fms.domain.AppConfig;
import com.fms.domain.AppUser;
import com.fms.domain.OrderType;
import com.fms.repo.AppUserRepository;
import com.fms.service.ConfigService;
import java.security.Principal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page template can use (replaces what Main.Master read from Session). */
@ControllerAdvice
public class GlobalModel {

    private final ConfigService configService;
    private final AppUserRepository users;

    public GlobalModel(ConfigService configService, AppUserRepository users) {
        this.configService = configService;
        this.users = users;
    }

    @ModelAttribute("config")
    public AppConfig config() {
        return configService.get();
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser(Principal principal) {
        return principal == null ? null : users.findByUsername(principal.getName()).orElse(null);
    }

    @ModelAttribute("orderTypes")
    public OrderType[] orderTypes() {
        return OrderType.values();
    }

    /**
     * Never let a form post set primary keys or audit fields; controllers set
     * those themselves. Stops e.g. an edit form from overwriting another row.
     */
    @InitBinder
    public void protectFields(WebDataBinder binder) {
        binder.setDisallowedFields("id", "registeredAt", "createdAt", "createdBy", "password", "lastLoginAt");
    }
}
