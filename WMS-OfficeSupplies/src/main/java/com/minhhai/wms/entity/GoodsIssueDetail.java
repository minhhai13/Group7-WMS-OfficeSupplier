package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Entity
@Table(name = "GoodsIssueDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GIDetailID")
    private Integer giDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GINID", nullable = false)
    private GoodsIssueNote goodsIssueNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SODetailID")
    private SalesOrderDetail salesOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TODetailID")
    private TransferOrderDetail transferOrderDetail;

    @Builder.Default
    @PositiveOrZero
    @Column(name = "PlannedQty", nullable = false, columnDefinition = "int default 0")
    private Integer plannedQty = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @PositiveOrZero
    @Column(name = "IssuedQty", nullable = false)
    private Integer issuedQty;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @Column(name = "BatchNumber", length = 50, nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BinID", nullable = false)
    private Bin bin;
}