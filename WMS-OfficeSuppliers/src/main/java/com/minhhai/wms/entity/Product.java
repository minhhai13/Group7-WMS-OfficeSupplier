package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "SKU", length = 50, nullable = false, unique = true, updatable = false)
    private String sku;

    @Column(name = "ProductName", length = 200, nullable = false)
    private String productName;

    @Column(
            name = "BaseUoM",
            length = 10,
            nullable = false,
            columnDefinition = "nvarchar(10) default 'EA'"
    )
    private String baseUoM = "EA"; // Base Unit of Measure (đơn vị nhỏ nhất)

    @Column(name = "MinStockLevel", columnDefinition = "int default 0")
    private Integer minStockLevel = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductUoMConversion> uomConversions;

    @OneToMany(mappedBy = "assignedProduct", fetch = FetchType.LAZY)
    private List<Bin> assignedBins;

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
    private List<TransferDetail> transferDetails;

    @OneToMany(mappedBy = "product")
    private List<StockBatch> stockBatches; // Để xem tồn kho chi tiết của sản phẩm này ở mọi nơi

    @OneToMany(mappedBy = "product")
    private List<StockMovement> stockMovements;
}