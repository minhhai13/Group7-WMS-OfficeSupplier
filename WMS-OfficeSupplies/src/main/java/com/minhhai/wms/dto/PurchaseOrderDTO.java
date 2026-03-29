package com.minhhai.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDTO {

    private Integer poId;

    private String poNumber;

    @NotNull(message = "Vui lòng chọn nhà cung cấp")
    private Integer supplierId;

    private String supplierName;

    private Integer warehouseId;

    private String warehouseName;

    private String poStatus;

    private String rejectReason;

    // PR source info: comma-separated PR numbers that generated this PO
    private String sourcePRNumbers;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất một dòng sản phẩm")
    @Builder.Default
    private List<PurchaseOrderDetailDTO> details = new ArrayList<>();
}
