package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GoodsReceiptDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GRDetailID")
    private Integer grDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GRNID", nullable = false)
    private GoodsReceiptNote goodsReceiptNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PODetailID", nullable = false)
    private PurchaseOrderDetail purchaseOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "ReceivedQty", nullable = false)
    private Integer receivedQty;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @Column(name = "BatchNumber", length = 50, nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BinID", nullable = false)
    private Bin bin;
}