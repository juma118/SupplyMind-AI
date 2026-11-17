package com.supplymind.core.supplier;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Suppliers screen (apps/web) deliberately calls this endpoint rather than
 * re-implementing the thresholds client-side, specifically to avoid the risk logic having
 * two homes that could drift. These boundary cases are exactly where that kind of drift
 * would first show up as a wrong color/label in the UI.
 */
class SupplierDtosTest {

    @Test
    void exactlyNinetyPercentIsLowRiskNotMedium() {
        assertThat(riskFor("0.90")).isEqualTo("LOW");
    }

    @Test
    void justBelowNinetyPercentIsMediumRisk() {
        assertThat(riskFor("0.8999")).isEqualTo("MEDIUM");
    }

    @Test
    void exactlySeventyFivePercentIsMediumRiskNotHigh() {
        assertThat(riskFor("0.75")).isEqualTo("MEDIUM");
    }

    @Test
    void justBelowSeventyFivePercentIsHighRisk() {
        assertThat(riskFor("0.7499")).isEqualTo("HIGH");
    }

    @Test
    void noRecordedPerformanceIsUnknownNotHigh() {
        Supplier s = new Supplier();
        s.setOnTimeRate(null);

        var result = SupplierDtos.SupplierPerformanceResponse.from(s);

        assertThat(result.riskLevel()).isEqualTo("UNKNOWN");
    }

    private String riskFor(String onTimeRate) {
        Supplier s = new Supplier();
        s.setOnTimeRate(new BigDecimal(onTimeRate));
        return SupplierDtos.SupplierPerformanceResponse.from(s).riskLevel();
    }
}
