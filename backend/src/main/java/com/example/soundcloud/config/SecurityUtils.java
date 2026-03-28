package com.example.soundcloud.config;

import com.example.soundcloud.model.User;
import com.example.soundcloud.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CurrentUser> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser cu) {
            return Optional.of(cu);
        }
        return Optional.empty();
    }

    public static Optional<User> getCurrentUser(UserService userService) {
        return getCurrentUserId()
                .flatMap(cu -> userService.findById(cu.userId()));
    }
}
