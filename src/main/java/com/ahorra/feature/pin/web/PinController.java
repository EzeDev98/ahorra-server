package com.ahorra.feature.pin.web;

import com.ahorra.feature.pin.dto.ChangePinRequest;
import com.ahorra.feature.pin.dto.PinRequest;
import com.ahorra.feature.pin.dto.PinStatusResponse;
import com.ahorra.feature.pin.dto.VerifyPinRequest;
import com.ahorra.feature.pin.service.PinService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authenticated by the default rule in WebSecurityConfig (no permitAll entry).
 * Verify and remove are POSTs on purpose: a PIN must never travel in a URL.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.title}/pin")
@Tag(name = "PIN API", description = "App-lock PIN: status, set, change, verify, remove and replace after sign-in")
public class PinController {

    private final PinService pinService;

    @GetMapping("/status")
    public ResponseEntity<PinStatusResponse> status() {
        return ResponseEntity.ok(pinService.status());
    }

    @PostMapping
    public ResponseEntity<PinStatusResponse> set(@RequestBody PinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pinService.setPin(request));
    }

    @PutMapping
    public ResponseEntity<PinStatusResponse> change(@RequestBody ChangePinRequest request) {
        return ResponseEntity.ok(pinService.changePin(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify(@RequestBody VerifyPinRequest request) {
        pinService.verifyPin(request);
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @PostMapping("/remove")
    public ResponseEntity<PinStatusResponse> remove(@RequestBody VerifyPinRequest request) {
        return ResponseEntity.ok(pinService.removePin(request));
    }

    @PostMapping("/reset")
    public ResponseEntity<PinStatusResponse> reset(@RequestBody PinRequest request) {
        return ResponseEntity.ok(pinService.resetPin(request));
    }
}
