package com.minhhai.wms.entity;

import jakarta.persistence.*;
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
    @JoinColumn(name = "SODetailID", nullable = false)
    private SalesOrderDetail salesOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

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