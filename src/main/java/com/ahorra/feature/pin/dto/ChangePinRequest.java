package com.ahorra.feature.pin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePinRequest {
    private String oldPin;
    private String pin;
    private String confirmPin;
}
