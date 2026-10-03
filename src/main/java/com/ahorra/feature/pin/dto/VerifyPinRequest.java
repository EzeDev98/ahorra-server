package com.ahorra.feature.pin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body for checking a PIN and for removing it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPinRequest {
    private String pin;
}
