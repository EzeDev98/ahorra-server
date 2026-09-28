package com.ahorra.feature.auth.service.impl;

import com.ahorra.common.exception.RegistrationException;
import com.ahorra.feature.auth.dto.request.RegistrationRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.ahorra.common.constants.GeneralConstants.DATE_FORMAT;

/**
 * Validates new-user registration requests.
 * <p>
 * Extracted from the original grab-bag validator as part of the SOLID
 * refactor — that class validated six unrelated domains (registration,
 * PIN setting, password reset, OTP format, transactions, and a generic
 * required-fields check) in one place, despite no consumer ever needing
 * more than one of those domains. This class holds exactly the
 * registration rules, used only by {@code UserServiceImpl}. No rule
 * changed during the move.
 * <p>
 * Date of birth is validated here, not in {@code UserServiceImpl#createUser},
 * specifically so it fails before {@code authClient.register(...)} is ever
 * called: that internal-auth-server call creates a real account and sends
 * a real verification OTP, so a bad date format or an under-18 date must
 * never be discovered only after that account already exists.
 */
@Component
public class RegistrationValidator {

    public void validateRegistrationRequest(RegistrationRequest request) {

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new RegistrationException("First name is required");
        }

        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new RegistrationException("Last name is required");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RegistrationException("Username is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RegistrationException("Email is required");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new RegistrationException("Phone number is required");
        }

        if (!request.getPhoneNumber().matches("^\\+?[0-9]{11}$")) {
            throw new RegistrationException("Phone number must be 11 digits and can start with +");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RegistrationException("Password is required");
        }

        if (request.getPassword().length() < 8) {
            throw new RegistrationException("Password must be at least 8 characters");
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new RegistrationException("Confirm password is required");
        }

        if (!request.getConfirmPassword().equals(request.getPassword())) {
            throw new RegistrationException("Passwords do not match");
        }

        if (request.getDateOfBirth() == null || request.getDateOfBirth().isBlank()) {
            throw new RegistrationException("Date of birth is required");
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (DateTimeParseException ex) {
            throw new RegistrationException("Date of birth must be in yyyy-MM-dd format, e.g. 2000-01-01");
        }

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            throw new RegistrationException("User must be at least 18 years old to register.");
        }
    }
}
