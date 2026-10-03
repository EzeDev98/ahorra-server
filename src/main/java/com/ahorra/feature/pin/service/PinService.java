package com.ahorra.feature.pin.service;

import com.ahorra.feature.pin.dto.ChangePinRequest;
import com.ahorra.feature.pin.dto.PinRequest;
import com.ahorra.feature.pin.dto.PinStatusResponse;
import com.ahorra.feature.pin.dto.VerifyPinRequest;

/** The signed-in user's app-lock PIN. Every method acts on the caller; none takes a user id. */
public interface PinService {

    PinStatusResponse status();

    PinStatusResponse setPin(PinRequest request);

    PinStatusResponse changePin(ChangePinRequest request);

    /** Checks the PIN. Returns normally when it matches; throws when it does not or when locked. */
    void verifyPin(VerifyPinRequest request);

    /** Removes the PIN, which needs the current PIN. */
    PinStatusResponse removePin(VerifyPinRequest request);

    /** Replaces a forgotten PIN. Allowed only shortly after signing in again. */
    PinStatusResponse resetPin(PinRequest request);

    /** Records a successful sign-in, so a forgotten PIN can be replaced. */
    void markFreshLogin(Long userId);
}
