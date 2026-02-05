package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "StockBatches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"WarehouseID", "ProductID", "BinID", "BatchNumber"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StockBatchID")
    private Integer stockBatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BinID", nullable = false)
    private Bin bin;

    @Column(name = "BatchNumber", length = 50, nullable = false)
    private String batchNumber;

    @Column(name = "ArrivalDateTime", nullable = false, columnDefinition = "datetime2")
    private LocalDateTime arrivalDateTime;

    // CRITICAL: Stock LUÔN lưu theo BaseUoM
    @Column(name = "QtyAvailable", nullable = false, columnDefinition = "int default 0")
    private Integer qtyAvailable = 0;

    @Column(name = "QtyReserved", nullable = false, columnDefinition = "int default 0")
    private Integer qtyReserved = 0;

    @Column(name = "QtyInTransit", nullable = false, columnDefinition = "int default 0")
    private Integer qtyInTransit = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom; // Phải = Products.BaseUoM

}