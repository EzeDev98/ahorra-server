package com.ahorra.feature.report.service;

import com.ahorra.feature.report.dto.ReportFacts;

/** Turns finished, backend-computed facts into one paragraph. Never computes anything itself. */
public interface NarrativeWriter {

    /** Returns the paragraph, or throws if it cannot be produced. */
    String write(ReportFacts facts);
}
