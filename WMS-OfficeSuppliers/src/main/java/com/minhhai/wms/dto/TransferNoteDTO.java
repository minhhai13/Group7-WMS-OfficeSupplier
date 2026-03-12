package com.minhhai.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferNoteDTO {
    private Integer tnId;
    private String tnNumber;
    private Integer warehouseId;
    private String warehouseName;
    private String status;

    @Valid
    @NotEmpty(message = "Transfer note must have details")
    private List<TransferNoteDetailDTO> details;
}
