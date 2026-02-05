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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierID", nullable = false)
    private Partner supplier;

    @Column(
            name = "POStatus",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Pending' CHECK ([POStatus] IN ('Pending', 'Approved', 'Rejected', 'Incomplete', 'Complete'))"
    )
    private String poStatus = "Pending"; // Pending, Approved, Rejected, Incomplete, Complete

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseOrderDetail> details;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsReceiptNote> goodsReceiptNotes;
}