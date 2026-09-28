package com.ahorra.feature.admin.dto;

import com.ahorra.feature.admin.web.AdminResetController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for the destructive data wipe.
 *
 * @see AdminResetController
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WipeRequest {

    /** Must equal the required confirmation phrase, or the request is rejected. */
    private String confirmation;
}