package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Bins",
        uniqueConstraints = @UniqueConstraint(columnNames = {"WarehouseID", "BinLocation"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BinID")
    private Integer binId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @Column(
            name = "BinLocation",
            length = 50,
            nullable = false,
            columnDefinition = "nvarchar(50) CHECK (BinLocation LIKE 'W[0-9][0-9]-Z[0-9][0-9]-S[0-9][0-9]-B[0-9][0-9]')"
    )
    @Pattern(
            regexp = "^W[0-9]{2}-Z[0-9]{2}-S[0-9]{2}-B[0-9]{2}$",
            message = "Bin Location phải đúng định dạng Wxx-Zxx-Sxx-Bxx (VD: W01-Z02-S01-B05)"
    )
    private String binLocation; // Format: W01-Z02-S01-B05

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedProductID")
    private Product assignedProduct;

    @Column(name = "MaxCapacity")
    private Integer maxCapacity;

    @OneToMany(mappedBy = "bin")
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "bin")
    private List<StockMovement> stockMovements;

    @OneToMany(mappedBy = "bin")
    private List<GoodsReceiptDetail> receiptDetails;

    @OneToMany(mappedBy = "bin")
    private List<GoodsIssueDetail> issueDetails;

    @OneToMany(mappedBy = "fromBin")
    private List<TransferDetail> outgoingTransferDetails;

    @OneToMany(mappedBy = "toBin")
    private List<TransferDetail> incomingTransferDetails;
}