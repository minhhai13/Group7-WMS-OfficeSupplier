package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsIssueDetail;
import com.minhhai.wms.dto.OutboundReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsIssueDetailRepository extends JpaRepository<GoodsIssueDetail, Integer> {

    // Đã fix lỗi lặp dòng bằng cách dùng GROUP BY
    @Query("SELECT new com.minhhai.wms.dto.OutboundReportDTO(" +
            "MAX(sm.movementDate), gin.ginNumber, p.sku, p.productName, gid.issuedQty, b.binLocation, gid.batchNumber) " +
            "FROM GoodsIssueDetail gid " +
            "JOIN gid.goodsIssueNote gin JOIN gid.product p JOIN gid.bin b " +
            "LEFT JOIN StockMovement sm ON sm.batchNumber = gid.batchNumber AND sm.movementType = 'Issue' " +
            "WHERE gin.warehouse.warehouseId = :warehouseId AND gin.giStatus = 'Posted' " +
            "GROUP BY gin.ginNumber, p.sku, p.productName, gid.issuedQty, b.binLocation, gid.batchNumber " +
            "ORDER BY MAX(sm.movementDate) DESC")
    Page<OutboundReportDTO> getOutboundReportByWarehouse(@Param("warehouseId") Integer warehouseId, Pageable pageable);
}