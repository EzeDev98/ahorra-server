package com.ahorra.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Role checks for @PreAuthorize expressions, e.g.
 * {@code @PreAuthorize("@rolesEvaluator.isAdmin(authentication)")}.
 * <p>
 * The JWT roles converter lowercases every role it reads, so the configured admin
 * role is lowercased here too. If no admin role is configured, nobody is an admin.
 */
@Component("rolesEvaluator")
public class RolesEvaluator {

    private final String adminRole;

    public RolesEvaluator(@Value("${app.security.admin-role:admin_role}") String adminRole) {
        this.adminRole = adminRole == null ? "" : adminRole.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || adminRole.isEmpty()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(adminRole::equals);
    }

    public boolean deleteTransactions(Authentication authentication) {
        return isAdmin(authentication);
    }

    public boolean getAllTransactions(Authentication authentication) {
        return isAdmin(authentication);
    }

    public boolean invalidateSession(Authentication authentication) {
        return isAdmin(authentication);
    }
}
