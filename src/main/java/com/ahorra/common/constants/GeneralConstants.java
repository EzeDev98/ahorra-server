package com.ahorra.common.constants;

import java.math.BigDecimal;
import java.time.Duration;

public class GeneralConstants {

    public static final String GENERIC_DOWNSTREAM_ERROR = "internal-auth-server request failed";

    public static final String WIPE_CONFIRMATION = "WIPE-EVERYTHING";

    public static final int MAX_PIN_CHANGE_ATTEMPTS = 3;

    public static final int PIN_CHANGE_LOCK_MINUTES = 15;

    public static final int RANDOM_STRING_LENGTH = 12;

    public static final int MAX_LOGIN_ATTEMPTS = 5;

    public static final BigDecimal ZERO = BigDecimal.ZERO;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String CURRENCY = "NGN";

    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static int MAX_ATTEMPTS = 5;

    public static final String REPLACED_BY_NEW_LOGIN = "REPLACED_BY_NEW_LOGIN";

    public static final String LOGOUT = "LOGOUT";

    public static final String ADMIN_ACTION = "ADMIN_ACTION";

    public static final String TTL_EXPIRED = "TTL_EXPIRED";

    public static final String PASSWORD_RESET_COOLDOWN_KEY_PREFIX = "password-reset:otp-cooldown:";

    public static final String PIN_RESET_COOLDOWN_KEY_PREFIX = "pin-reset:otp-cooldown:";

    public static final String GENERIC_REQUEST_MESSAGE = "If an account is associated with this phone number, a verification code has been sent.";

    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public static final int RESET_TOKEN_BYTES = 32;

    public static final long SESSION_TTL_DAYS = 60;

    public static final int LOGIN_RATE_LIMIT_DURATION = 15;

    public static final int RESET_PASSWORD_RATE_LIMIT_DURATION = 24;

    public static final int RESET_PIN_RATE_LIMIT_DURATION = 24;

    public static final int MAX_LOGIN_REQUEST = 15;

    public static final int MAX_RESET_PASSWORD_REQUEST = 5;

    public static final int MAX_RESET_PIN_REQUEST = 5;

    private GeneralConstants() {
        throw new UnsupportedOperationException("General constant class and cannot be instantiated");
    }
}