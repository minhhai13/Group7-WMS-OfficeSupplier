package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "GoodsIssueNotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GINID")
    private Integer ginId;

    @Column(name = "GINNumber", length = 20, nullable = false, unique = true, updatable = false)
    private String ginNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOID", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @Column(
            name = "GIStatus",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Draft' CHECK ([GIStatus] IN ('Draft', 'Posted'))"
    )
    private String giStatus = "Draft"; // Draft, Posted

    @OneToMany(mappedBy = "goodsIssueNote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsIssueDetail> details;
}