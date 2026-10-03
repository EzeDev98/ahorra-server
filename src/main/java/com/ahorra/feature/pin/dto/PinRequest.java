package com.ahorra.feature.pin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body for setting a PIN and for replacing a forgotten one. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PinRequest {
    private String pin;
    private String confirmPin;
}
