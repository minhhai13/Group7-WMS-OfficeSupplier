package com.minhhai.wms.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueNoteDTO {

    private Integer ginId;
    private String ginNumber;
    private String soNumber;
    private String customerName;
    private String warehouseName;
    private String giStatus;
    private List<GoodsIssueDetailDTO> details;
}
