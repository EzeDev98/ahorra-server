package com.ahorra.feature.capture.dto;

import com.ahorra.feature.capture.enums.EntrySource;

/**
 * Body of POST /entries/capture. Source defaults to TEXT when omitted.
 * clientRequestId is optional: sending the same value again returns the
 * already-created pending entry instead of parsing a second time.
 */
public record CaptureRequest(String text, EntrySource source, String clientRequestId) {
}
