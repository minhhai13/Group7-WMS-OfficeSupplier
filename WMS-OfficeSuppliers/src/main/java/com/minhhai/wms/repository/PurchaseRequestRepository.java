package com.minhhai.wms.repository;

import com.minhhai.wms.entity.PurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Integer> {

    Optional<PurchaseRequest> findByRelatedSalesOrder_SoId(Integer soId);

    List<PurchaseRequest> findByPurchaseOrder_PoId(Integer poId);

    Page<PurchaseRequest> findByWarehouse_WarehouseId(Integer warehouseId, Pageable pageable);

    Page<PurchaseRequest> findByWarehouse_WarehouseIdAndStatus(Integer warehouseId, String status, Pageable pageable);

    List<PurchaseRequest> findByWarehouse_WarehouseIdAndStatus(Integer warehouseId, String status);

    List<PurchaseRequest> findByRelatedSalesOrder_SoIdAndStatusIn(Integer soId, List<String> statuses);

    @Query("SELECT MAX(pr.prNumber) FROM PurchaseRequest pr WHERE pr.prNumber LIKE :prefix%")
    String findMaxPrNumber(@Param("prefix") String prefix);
}
