package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferNoteDetailDTO {
    private Integer tnDetailId;

    @NotNull(message = "Product is required")
    private Integer productId;
    private String productDisplayName;

    @NotNull(message = "Batch number is required")
    private String batchNumber;

    @NotNull(message = "From bin is required")
    private Integer fromBinId;
    private String fromBinLocation;

    @NotNull(message = "To bin is required")
    private Integer toBinId;
    private String toBinLocation;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "UoM is required")
    private String uom;
}
