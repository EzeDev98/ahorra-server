package com.ahorra.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.UUID;

import static com.ahorra.common.constants.GeneralConstants.RANDOM_STRING_LENGTH;

@Component
@RequiredArgsConstructor
public class UniqueValueGeneratorService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateWalletId() {
        UUID uuid = UUID.randomUUID();
        return "AH_" + uuid.toString().replace("-", "").substring(0, 6);
    }

    public String generateSyncId() {
        String timeComponent = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String randomString = generateRandomString(RANDOM_STRING_LENGTH);
        return timeComponent + "_" + randomString;
    }

    public String generateTransactionReference() {
        String timeComponent = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String randomString = generateRandomString(RANDOM_STRING_LENGTH);
        return timeComponent + "_" + randomString;
    }

    public String generateRequestReference() {
        String timeComponent = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String randomString = generateRandomString(RANDOM_STRING_LENGTH);
        return timeComponent + "_" + randomString;
    }

    public String generateRandomString(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomString = toBase36String(randomBytes);
        return randomString.substring(0, length).toUpperCase();
    }

    private String toBase36String(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString((b & 0xFF), 36));
        }
        return sb.toString();
    }

}
