package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WarehouseID")
    private Integer warehouseId;

    @NotBlank(message = "Mã kho không được để trống")
    @Size(max = 20)
    @Column(name = "WarehouseCode", length = 20, nullable = false, unique = true)
    private String warehouseCode;

    @NotBlank(message = "Tên kho không được để trống")
    @Size(max = 100)
    @Column(name = "WarehouseName", length = 100, nullable = false)
    private String warehouseName;

    @Size(max = 255)
    @Column(name = "Address", length = 255)
    private String address;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bin> bins;

    @OneToMany(mappedBy = "warehouse")
    private List<PurchaseOrder> purchaseOrders;

    @OneToMany(mappedBy = "warehouse")
    private List<SalesOrder> salesOrders;

    @OneToMany(mappedBy = "warehouse")
    private List<PurchaseRequest> purchaseRequests;

    @OneToMany(mappedBy = "warehouse")
    private List<GoodsReceiptNote> goodsReceiptNotes;

    @OneToMany(mappedBy = "warehouse")
    private List<GoodsIssueNote> goodsIssueNotes;

    @OneToMany(mappedBy = "sourceWarehouse")
    private List<Transfer> outgoingTransfers;

    @OneToMany(mappedBy = "destinationWarehouse")
    private List<Transfer> incomingTransfers;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "warehouse")
    private List<StockMovement> stockMovements;
}