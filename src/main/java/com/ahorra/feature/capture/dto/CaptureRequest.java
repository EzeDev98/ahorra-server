package com.ahorra.feature.capture.dto;

import com.ahorra.feature.capture.enums.EntrySource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /entries/capture. Source defaults to TEXT when omitted.
 * clientRequestId is optional: sending the same value again returns the
 * already-created pending entry instead of parsing a second time.
 */
public record CaptureRequest(
        @NotBlank(message = "Entry text is required")
        @Size(max = 2000, message = "Entry is too long (max 2000 characters)")
        String text,
        EntrySource source,
        @Size(max = 100, message = "clientRequestId is too long")
        String clientRequestId
) {
}
