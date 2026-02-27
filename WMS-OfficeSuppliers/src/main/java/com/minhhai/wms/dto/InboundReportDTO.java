package com.minhhai.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundReportDTO {
    private LocalDateTime inboundDate;
    private String grnNumber;
    private String skuCode;
    private String productName;
    private Integer receivedQuantity;
    private String assignedBin;
}
