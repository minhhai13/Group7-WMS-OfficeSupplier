package com.minhhai.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinDTO {

    private Integer binId;

    @NotBlank(message = "Bin location is required")
    @Pattern(
            regexp = "^W[0-9]{2}-Z[0-9]{2}-S[0-9]{2}-B[0-9]{2}$",
            message = "Bin location must match format Wxx-Zxx-Sxx-Bxx (e.g. W01-Z02-S01-B05)"
    )
    private String binLocation;

    @DecimalMin(value = "0.0", inclusive = false, message = "Max weight must be greater than 0")
    private BigDecimal maxWeight;

    private Integer warehouseId;

    private Boolean isActive;
}
