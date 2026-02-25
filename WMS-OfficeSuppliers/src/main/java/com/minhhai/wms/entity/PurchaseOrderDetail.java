package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @OneToMany(mappedBy = "purchaseOrderDetail")
    private List<PurchaseRequestDetail> purchaseRequestDetails;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(
            name = "OrderedQty",
            nullable = false,
            columnDefinition = "int CHECK (OrderedQty > 0)"
    )
    @Positive(message = "Ordered quantity must be greater than 0")
    private Integer orderedQty;

    @PositiveOrZero(message = "Received quantity must not be negative")
    @Builder.Default
    @Column(
            name = "ReceivedQty",
            columnDefinition = "int default 0 CHECK (ReceivedQty >= 0 AND ReceivedQty <= OrderedQty)"
    )
    private Integer receivedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @OneToMany(mappedBy = "purchaseOrderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsReceiptDetail> goodsReceiptDetails;

    @AssertTrue(message = "Received quantity must not exceed ordered quantity")
    private boolean isReceivedNotExceedingOrdered() {
        if (receivedQty == null || orderedQty == null) return true;
        return receivedQty <= orderedQty;
    }
}