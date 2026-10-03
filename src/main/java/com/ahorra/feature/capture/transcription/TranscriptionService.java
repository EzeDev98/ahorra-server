package com.ahorra.feature.capture.transcription;

/** Turns a short voice recording into text. The audio is used for the call and then dropped; nothing is stored. */
public interface TranscriptionService {

    /**
     * @param audio       the recording as sent by the browser
     * @param contentType its media type, for example audio/webm or audio/mp4
     * @return the transcript, trimmed; empty when nothing could be heard
     */
    String transcribe(byte[] audio, String contentType);
}
