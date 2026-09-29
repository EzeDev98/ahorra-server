package com.ahorra.feature.report.service;

import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.income.dto.VarianceResponse;
import com.ahorra.feature.report.enums.ReportVerdict;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The single definition of over / on-track / under, shared by the daily report
 * and the ambient strip. Judged against the lower-bound baseline: OVER when
 * spending has already passed it, or runs more than the tolerance above the
 * pace the baseline implies; UNDER when more than the tolerance below that pace.
 */
@Component
public class VerdictCalculator {

    /** Tolerance around the expected pace. Tune here. */
    static final BigDecimal UPPER = new BigDecimal("1.10");
    static final BigDecimal LOWER = new BigDecimal("0.90");

    public ReportVerdict verdict(VarianceResponse v) {
        if (MoneyUtils.isPositive(v.variance())) {
            return ReportVerdict.OVER;
        }
        BigDecimal expectedSoFar = MoneyUtils.divide(
                MoneyUtils.multiply(v.baseline(), BigDecimal.valueOf(v.daysElapsed())),
                BigDecimal.valueOf(v.daysInPeriod()));

        if (MoneyUtils.isGreaterThan(v.spend(), MoneyUtils.multiply(expectedSoFar, UPPER))) {
            return ReportVerdict.OVER;
        }
        if (MoneyUtils.isLessThan(v.spend(), MoneyUtils.multiply(expectedSoFar, LOWER))) {
            return ReportVerdict.UNDER;
        }
        return ReportVerdict.ON_TRACK;
    }
}
