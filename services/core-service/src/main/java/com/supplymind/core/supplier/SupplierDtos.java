package com.supplymind.core.supplier;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record CreateSupplierRequest(
            @NotBlank String name,
            String category,
            BigDecimal onTimeRate,
            Integer avgLeadTimeDays) {
    }

    public record SupplierResponse(
            UUID id,
            String name,
            String category,
            BigDecimal onTimeRate,
            Integer avgLeadTimeDays,
            Instant createdAt,
            Instant updatedAt) {

        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getCategory(),
                    s.getOnTimeRate(), s.getAvgLeadTimeDays(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    public record SupplierPerformanceResponse(
            UUID supplierId,
            String name,
            BigDecimal onTimeRate,
            Integer avgLeadTimeDays,
            String riskLevel) {

        public static SupplierPerformanceResponse from(Supplier s) {
            String risk;
            BigDecimal rate = s.getOnTimeRate();
            if (rate == null) {
                risk = "UNKNOWN";
            } else if (rate.compareTo(new BigDecimal("0.90")) >= 0) {
                risk = "LOW";
            } else if (rate.compareTo(new BigDecimal("0.75")) >= 0) {
                risk = "MEDIUM";
            } else {
                risk = "HIGH";
            }
            return new SupplierPerformanceResponse(s.getId(), s.getName(), s.getOnTimeRate(),
                    s.getAvgLeadTimeDays(), risk);
        }
    }
}
