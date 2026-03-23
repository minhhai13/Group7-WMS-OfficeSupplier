package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
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
    @JoinColumn(name = "PODetailID")
    private PurchaseOrderDetail purchaseOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TODetailID")
    private TransferOrderDetail transferOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @PositiveOrZero
    @Column(name = "ReceivedQty", nullable = false)
    private Integer receivedQty;

    @Builder.Default
    @PositiveOrZero
    @Column(name = "ExpectedQty", nullable = false, columnDefinition = "int default 0")
    private Integer expectedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @Column(name = "BatchNumber", length = 50, nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BinID", nullable = false)
    private Bin bin;
}