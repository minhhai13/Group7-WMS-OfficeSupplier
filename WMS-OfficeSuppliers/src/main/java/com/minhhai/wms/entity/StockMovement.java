package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "StockMovements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovementID")
    private Integer movementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BinID", nullable = false)
    private Bin bin;

    @Column(name = "BatchNumber", length = 50)
    private String batchNumber;

    @Column(
            name = "MovementType",
            length = 30,
            nullable = false,
            columnDefinition = "nvarchar(30) CHECK ([MovementType] IN ('Receipt', 'Issue', 'Transfer-Out', 'Transfer-In', 'Reserve', 'Release'))"
    )
    private String movementType; // Receipt, Issue, Transfer-In, Transfer-Out, Reserve, Release

    @Column(
            name = "StockType",
            length = 20,
            nullable = false,
            columnDefinition = "nvarchar(20) CHECK ([StockType] IN ('Physical', 'Reserved', 'In-Transit'))"
    )
    // Physical, Reserved, In-Transit
    private String stockType;

    @Column(name = "MovementDate", insertable = false, updatable = false,
            columnDefinition = "datetime2 default getdate()")
    private LocalDateTime movementDate;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom; // Lưu theo BaseUoM

    @Column(name = "BalanceAfter", nullable = false)
    private Integer balanceAfter;
}