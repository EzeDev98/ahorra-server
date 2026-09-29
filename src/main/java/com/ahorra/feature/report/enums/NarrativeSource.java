package com.ahorra.feature.report.enums;

public enum NarrativeSource {
    /** Paragraph written by the model and verified against the computed facts. */
    AI,
    /** Deterministic paragraph, used when the model failed or its output could not be verified. */
    TEMPLATE
}
