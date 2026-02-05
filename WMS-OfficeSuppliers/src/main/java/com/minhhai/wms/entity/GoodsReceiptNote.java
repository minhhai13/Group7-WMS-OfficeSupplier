package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "GoodsReceiptNotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GRNID")
    private Integer grnId;

    @Column(name = "GRNNumber", length = 20, nullable = false, unique = true, updatable = false)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POID", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @Column(
            name = "GRStatus",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Draft' CHECK ([GRStatus] IN ('Draft', 'Posted'))"
    )
    private String grStatus = "Draft"; // Draft, Posted

    @OneToMany(mappedBy = "goodsReceiptNote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsReceiptDetail> details;
}