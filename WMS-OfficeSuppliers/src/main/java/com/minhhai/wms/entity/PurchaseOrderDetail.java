package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "PurchaseOrderDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PODetailID")
    private Integer poDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POID", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRDetailID")
    private PurchaseRequestDetail purchaseRequestDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(
            name = "OrderedQty",
            nullable = false,
            columnDefinition = "int CHECK (OrderedQty > 0)"
    )
    private Integer orderedQty;

    @Column(
            name = "ReceivedQty",
            columnDefinition = "int default 0 CHECK (ReceivedQty >= 0 AND ReceivedQty <= OrderedQty)"
    )
    private Integer receivedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @OneToMany(mappedBy = "purchaseOrderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsReceiptDetail> goodsReceiptDetails;
}