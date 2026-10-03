package com.ahorra.feature.capture.dto;

/** The words heard in a recording. Nothing is saved: the client shows them for review, then sends them as a normal entry. */
public record TranscriptResponse(String text) {
}
