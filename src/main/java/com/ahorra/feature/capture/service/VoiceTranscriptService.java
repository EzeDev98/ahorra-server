package com.ahorra.feature.capture.service;

import com.ahorra.feature.capture.dto.TranscriptResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceTranscriptService {

    /** Turns a short recording into text and returns it. Nothing is stored and no entry is created. */
    TranscriptResponse transcribe(Long userId, MultipartFile audio);
}
