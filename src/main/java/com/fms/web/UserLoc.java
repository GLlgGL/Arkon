package com.fms.web;

import com.fms.repo.AppUserRepository;
import org.springframework.stereotype.Component;

/** For the user form: a user's own location id (${@userLoc.of(id)}). */
@Component("userLoc")
public class UserLoc {

    private final AppUserRepository users;

    public UserLoc(AppUserRepository users) {
        this.users = users;
    }

    /** ALL, OWN or "" (as the role) — for the user form. */
    public String scopeOf(Long userId) {
        return userId == null ? "" : users.findById(userId).map(u -> u.getRecordScope() == null ? "" : u.getRecordScope()).orElse("");
    }

    public Long of(Long userId) {
        return userId == null ? null : users.findById(userId).map(u -> u.getLocationId()).orElse(null);
    }
}
