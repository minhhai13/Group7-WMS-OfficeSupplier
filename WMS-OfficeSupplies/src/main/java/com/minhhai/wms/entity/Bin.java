package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
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

    @NotBlank(message = "Bin location is required")
    @Column(
            name = "BinLocation",
            length = 50,
            nullable = false,
            columnDefinition = "nvarchar(50) CHECK (BinLocation LIKE 'W[0-9][0-9]-Z[0-9][0-9]-S[0-9][0-9]-B[0-9][0-9]')"
    )
    @Pattern(
            regexp = "^W[0-9]{2}-Z[0-9]{2}-S[0-9]{2}-B[0-9]{2}$",
            message = "Bin location must match format Wxx-Zxx-Sxx-Bxx (e.g. W01-Z02-S01-B05)"
    )
    private String binLocation; // Định dạng: W01-Z02-S01-B05

    @Column(name = "MaxWeight", nullable = false, precision = 10, scale = 2, columnDefinition = "decimal(10,2) default 0")
    private BigDecimal maxWeight; // Khớp với DECIMAL(10, 2) trong DB

    @Builder.Default
    @Column(name = "IsActive", nullable = false, columnDefinition = "bit default 1")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "bin")
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "bin")
    private List<StockMovement> stockMovements;

    @OneToMany(mappedBy = "bin")
    private List<GoodsReceiptDetail> receiptDetails;

    @OneToMany(mappedBy = "bin")
    private List<GoodsIssueDetail> issueDetails;

    @OneToMany(mappedBy = "fromBin")
    private List<TransferNoteDetail> outgoingTransferNoteDetails;

    @OneToMany(mappedBy = "toBin")
    private List<TransferNoteDetail> incomingTransferNoteDetails;
}