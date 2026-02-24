package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "PurchaseOrders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POID")
    private Integer poId;

    @Column(name = "PONumber", length = 20, nullable = false, unique = true, updatable = false)
    private String poNumber;

    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.LAZY)
    private List<PurchaseRequest> purchaseRequests;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SupplierID", nullable = false)
    private Partner supplier;

    @Builder.Default
    @Column(
            name = "POStatus",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Draft' CHECK ([POStatus] IN ('Draft', 'Pending Approval', 'Approved', 'Rejected', 'Completed', 'Incomplete'))"
    )
    private String poStatus = "Draft"; // Draft, Pending Approval, Approved, Rejected, Completed, Incomplete

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PurchaseOrderDetail> details;

    @Column(name = "RejectReason", length = 500)
    private String rejectReason;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsReceiptNote> goodsReceiptNotes;
}