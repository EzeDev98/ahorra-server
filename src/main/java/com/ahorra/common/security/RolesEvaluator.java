package com.ahorra.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component("rolesEvaluator")
public class RolesEvaluator {

    public boolean deleteTransactions(Authentication authentication) {
        log.info("Authentication object type: {}", authentication.getClass().getName());
        log.info("Granted authorities for system endpoints: {}", authentication.getAuthorities());

        Set<String> requiredRoles = Set.of("ADMIN_ROLE");
        return authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet())
                .containsAll(requiredRoles);

    }

    public boolean getAllTransactions(Authentication authentication) {
        log.info("Authentication object type: {}", authentication.getClass().getName());
        log.info("Granted authorities for system endpoints: {}", authentication.getAuthorities());

        Set<String> requiredRoles = Set.of("ADMIN_ROLE");
        return authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet())
                .containsAll(requiredRoles);

    }

    public boolean invalidateSession(Authentication authentication) {
        log.info("Authentication object type: {}", authentication.getClass().getName());
        log.info("Granted authorities for system endpoints: {}", authentication.getAuthorities());

        Set<String> requiredRoles = Set.of("ADMIN_ROLE");
        return authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet())
                .containsAll(requiredRoles);
    }
}