package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "TransferNoteDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferNoteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TNDetailID")
    private Integer tnDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TNID", nullable = false)
    private TransferNote transferNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "BatchNumber", length = 50, nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FromBinID", nullable = false)
    private Bin fromBin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToBinID", nullable = false)
    private Bin toBin;

    @Positive(message = "Quantity must be greater than 0")
    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @AssertTrue(message = "From bin must be different from to bin")
    private boolean isDifferentBins() {
        if (fromBin == null || toBin == null) return true;
        return !fromBin.equals(toBin);
    }
}
