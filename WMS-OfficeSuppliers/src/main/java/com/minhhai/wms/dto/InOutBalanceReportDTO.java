package com.minhhai.wms.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InOutBalanceReportDTO {
    private LocalDateTime transactionDate;
    private String referenceDoc;
    private Integer inboundQty;
    private Integer outboundQty;
    private Integer openingBalance;
    private Integer closingBalance;
    private String binLocation;
    private String batchNumber;
}