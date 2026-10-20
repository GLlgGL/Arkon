package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.domain.AppUser;
import com.fms.domain.Area;
import com.fms.domain.UserAccess;
import com.fms.repo.AppUserRepository;
import com.fms.repo.UserAccessRepository;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may use what. Each role has default access per area (see Area); an
 * administrator can override it per user. An area of a switched-off module
 * is never accessible. Used by the request guard and by pages as
 * ${@perm.can('REPORTS')} / ${@perm.edit('REPORTS')}.
 */
@Service("perm")
public class AccessService {

    private final UserAccessRepository overrides;
    private final AppUserRepository users;
    private final ConfigService config;
    private final AuditService audit;

    public AccessService(UserAccessRepository overrides, AppUserRepository users, ConfigService config, AuditService audit) {
        this.overrides = overrides;
        this.users = users;
        this.config = config;
        this.audit = audit;
    }

    /** Effective access of a user to every area. */
    @Transactional(readOnly = true)
    public Map<Area, Area.Level> levels(AppUser u) {
        Map<Area, Area.Level> m = new EnumMap<>(Area.defaults(u));
        if (!u.isAdmin()) {
            for (UserAccess o : overrides.findByUsername(u.getUsername())) {
                m.put(o.getArea(), o.getLevel());
            }
        }
        AppConfig c = config.get();
        for (Area a : Area.values()) {
            if (!moduleOn(a, c)) {
                m.put(a, Area.Level.NONE);
            }
        }
        return m;
    }

    /** Overrides only (for the user form). */
    @Transactional(readOnly = true)
    public Map<Area, Area.Level> overridesOf(String username) {
        Map<Area, Area.Level> m = new EnumMap<>(Area.class);
        overrides.findByUsername(username).forEach(o -> m.put(o.getArea(), o.getLevel()));
        return m;
    }

    private static boolean moduleOn(Area a, AppConfig c) {
        return switch (a) {
            case CASH -> c.isModCash();
            case COST_CENTERS -> c.isModCostCenters();
            case PAYROLL -> c.isModPayroll();
            case ASSETS -> c.isModAssets();
            case PRODUCTION -> c.isModProduction();
            case TAXES -> c.isModTaxes();
            default -> true;
        };
    }

    public Area.Level level(AppUser u, Area a) {
        return levels(u).getOrDefault(a, Area.Level.NONE);
    }

    private AppUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : users.findByUsername(auth.getName()).orElse(null);
    }

    /** The current user may at least view this area. */
    public boolean can(String area) {
        AppUser u = current();
        return u != null && level(u, Area.valueOf(area)).atLeast(Area.Level.VIEW);
    }

    /** The current user may change things in this area. */
    public boolean edit(String area) {
        AppUser u = current();
        return u != null && level(u, Area.valueOf(area)).atLeast(Area.Level.FULL);
    }

    /** The current user may view at least one of these areas. */
    public boolean any(String... areas) {
        for (String a : areas) {
            if (can(a)) {
                return true;
            }
        }
        return false;
    }

    /** Replaces a user's overrides; areas equal to the role default are not stored. */
    @Transactional
    public void save(AppUser u, Map<Area, Area.Level> chosen) {
        overrides.deleteUser(u.getUsername());
        Map<Area, Area.Level> defaults = Area.defaults(u);
        int n = 0;
        for (var e : chosen.entrySet()) {
            if (e.getValue() != null && e.getValue() != defaults.get(e.getKey())) {
                UserAccess o = new UserAccess();
                o.setUsername(u.getUsername());
                o.setArea(e.getKey());
                o.setLevel(e.getValue());
                overrides.save(o);
                n++;
            }
        }
        audit.log("UPDATE", "User access", u.getUsername(), n + " changes from the role defaults");
    }

    @Transactional
    public void deleteUser(String username) {
        overrides.deleteUser(username);
    }
}
