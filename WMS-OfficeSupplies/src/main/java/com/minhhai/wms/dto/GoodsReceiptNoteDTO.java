package com.minhhai.wms.dto;

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
    private String poNumber;
    private String supplierName;
    private String warehouseName;
    private String grStatus;

    @Builder.Default
    private List<GoodsReceiptDetailDTO> details = new ArrayList<>();
}
