package com.fms.web;

import com.fms.domain.AppUser;
import com.fms.service.UserService;
import java.security.Principal;
import org.springframework.stereotype.Component;

/** Looks up the logged-in user's record for a request. */
@Component
public class CurrentUser {

    private final UserService users;

    public CurrentUser(UserService users) {
        this.users = users;
    }

    public AppUser of(Principal principal) {
        return users.get(principal.getName());
    }
}
