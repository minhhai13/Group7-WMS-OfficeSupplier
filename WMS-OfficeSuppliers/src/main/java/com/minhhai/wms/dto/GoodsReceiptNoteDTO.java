package com.minhhai.wms.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteDTO {
    private Integer grnId;
    private String grnNumber;
    private Integer poId;
    private String poNumber;
    private Integer warehouseId;
    private String grStatus;
    
    @Valid
    @Builder.Default
    private List<GoodsReceiptDetailDTO> details = new ArrayList<>();
}
