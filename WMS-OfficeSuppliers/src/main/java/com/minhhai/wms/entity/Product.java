package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @NotBlank(message = "SKU is required")
    @Column(name = "SKU", length = 50, nullable = false, unique = true)
    private String sku;

    @NotBlank(message = "Product name is required")
    @Column(name = "ProductName", length = 200, nullable = false)
    private String productName;

    @Column(name = "UnitWeight", nullable = false, precision = 10, scale = 2, columnDefinition = "decimal(10,2) default 0")
    private BigDecimal unitWeight;

    @NotBlank(message = "Base UoM is required")
    @Builder.Default
    @Column(
            name = "BaseUoM",
            length = 10,
            nullable = false,
            columnDefinition = "nvarchar(10) default 'EA'"
    )
    private String baseUoM = "EA"; // Base Unit of Measure (đơn vị nhỏ nhất)

    @Builder.Default
    @Column(name = "MinStockLevel", columnDefinition = "int default 0")
    private Integer minStockLevel = 0;

    @Builder.Default
    @Column(name = "IsActive", nullable = false, columnDefinition = "bit default 1")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductUoMConversion> uomConversions;

    @OneToMany(mappedBy = "product")
    private List<PurchaseRequestDetail> purchaseRequestDetails;

    @OneToMany(mappedBy = "product")
    private List<PurchaseOrderDetail> purchaseOrderDetails;

    @OneToMany(mappedBy = "product")
    private List<GoodsReceiptDetail> goodsReceiptDetails;

    @OneToMany(mappedBy = "product")
    private List<SalesOrderDetail> salesOrderDetails;

    @OneToMany(mappedBy = "product")
    private List<GoodsIssueDetail> goodsIssueDetails;

    @OneToMany(mappedBy = "product")
    private List<TransferOrderDetail> transferOrderDetails;

    @OneToMany(mappedBy = "product")
    private List<TransferNoteDetail> transferNoteDetails;

    @OneToMany(mappedBy = "product")
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "product")
    private List<StockMovement> stockMovements;
}