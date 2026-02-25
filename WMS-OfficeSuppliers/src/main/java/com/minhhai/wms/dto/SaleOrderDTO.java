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
public class SaleOrderDTO {

    private Integer soId;

    private String soNumber;

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Integer customerId;

    private String customerName;

    private Integer warehouseId;

    private String warehouseName;

    private String soStatus;

    private String rejectReason;

    // PR integration fields
    private boolean hasPR;
    private String prNumber;
    private String prStatus;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất một dòng sản phẩm")
    @Builder.Default
    private List<SaleOrderDetailDTO> details = new ArrayList<>();
}
