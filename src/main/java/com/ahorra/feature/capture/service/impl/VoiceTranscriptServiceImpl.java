package com.ahorra.feature.capture.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.feature.capture.dto.TranscriptResponse;
import com.ahorra.feature.capture.service.VoiceTranscriptService;
import com.ahorra.feature.capture.transcription.TranscriptionService;
import com.ahorra.feature.day.service.DayGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

import static com.ahorra.common.constants.GeneralConstants.MAX_TEXT_LENGTH;
import static com.ahorra.common.constants.GeneralConstants.MAX_VOICE_BYTES;

/**
 * Not @Transactional: no database connection is held open while waiting on the network. The recording is read into
 * memory, sent for transcription and dropped. Nothing is saved here; the person checks the text and sends it as a
 * normal entry with source VOICE, so confirmation still works exactly as for typed entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceTranscriptServiceImpl implements VoiceTranscriptService {

    private final TranscriptionService transcriptionService;
    private final DayGateService dayGateService;

    @Override
    public TranscriptResponse transcribe(Long userId, MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BadRequestException("The recording is empty. Please try again.");
        }
        if (audio.getSize() > MAX_VOICE_BYTES) {
            throw new BadRequestException("The recording is too long. Keep it under a minute.");
        }
        String contentType = audio.getContentType() == null ? "" : audio.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("audio/") && !contentType.startsWith("video/webm") && !contentType.startsWith("video/mp4")) {
            throw new BadRequestException("That file is not a voice recording.");
        }

        // A gated day cannot capture anything, so do not pay for a transcription it would refuse.
        dayGateService.assertNotGated(userId);

        byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("The recording could not be read. Please try again.");
        }

        String text = transcriptionService.transcribe(bytes, contentType);
        if (text.isBlank()) {
            throw new BadRequestException("We could not hear anything in that recording. Please try again.");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException("That recording is too long. Keep it to one or two sentences.");
        }
        return new TranscriptResponse(text);
    }
}
