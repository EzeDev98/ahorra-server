package com.ahorra.common.constants;

import java.math.BigDecimal;
import java.time.Duration;

public class GeneralConstants {

    public static final String GENERIC_DOWNSTREAM_ERROR = "internal-auth-server request failed";

    public static final String WIPE_CONFIRMATION = "WIPE-EVERYTHING";

    public static final int RANDOM_STRING_LENGTH = 12;

    public static final BigDecimal ZERO = BigDecimal.ZERO;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String REPLACED_BY_NEW_LOGIN = "REPLACED_BY_NEW_LOGIN";

    public static final String LOGOUT = "LOGOUT";

    public static final String ADMIN_ACTION = "ADMIN_ACTION";

    public static final String TTL_EXPIRED = "TTL_EXPIRED";

    public static final long SESSION_TTL_DAYS = 60;

    public static final int RESET_PASSWORD_RATE_LIMIT_DURATION = 24;

    public static final int MAX_RESET_PASSWORD_REQUEST = 5;

    public static final int[] WINDOWS = {7, 30, 90};

    public static final int MAX_WINDOW = 90;

    public static final int HABIT_WINDOW = 56;

    public static final String UNCATEGORIZED = "Uncategorized";

    public static final int JUDGMENT_DAYS = 30;

    public static final int LOOKBACK_DAYS = 90;

    public static final double UNUSUAL_Z = 2.0;

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    public static final String ALL_SPENDING = "All spending";

    public static final String TOOL_NAME = "record_entry";

    public static final String API_VERSION = "2023-06-01";

    public static final int MAX_TEXT_LENGTH = 2000;

    public static final int MAX_CLIENT_REQUEST_ID_LENGTH = 100;

    public static final int MAX_VENDOR_LENGTH = 255;

    public static final BigDecimal MAX_TOTAL = new BigDecimal("10000000000000");

    public static final int MAX_ITEMS = 50;

    public static final int MAX_DESCRIPTION_LENGTH = 255;

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    public static final int MAX_CUSTOM_PERIOD_DAYS = 366;

    public static final String NO_ENTRIES_SUMMARY = "No entries logged today";

    public static final int OBSERVATION_DAYS = 30;

    public static final int MAX_BACKFILL_DAYS = 14;

    public static final int MAX_INSIGHT_LENGTH = 500;

    public static final int PIN_LENGTH = 6;

    public static final int MAX_PIN_ATTEMPTS = 5;

    public static final int PIN_LOCK_MINUTES = 15;

    public static final int PIN_RESET_WINDOW_MINUTES = 10;

    private GeneralConstants() {
        throw new UnsupportedOperationException("General constant class and cannot be instantiated");
    }
}