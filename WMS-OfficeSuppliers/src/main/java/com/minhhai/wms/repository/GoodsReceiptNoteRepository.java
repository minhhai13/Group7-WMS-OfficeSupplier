package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Integer> {

    List<GoodsReceiptNote> findByWarehouse_WarehouseId(Integer warehouseId);

    List<GoodsReceiptNote> findByWarehouse_WarehouseIdAndGrStatus(Integer warehouseId, String grStatus);

    List<GoodsReceiptNote> findByPurchaseOrder_PoId(Integer poId);

    @Query("SELECT MAX(g.grnNumber) FROM GoodsReceiptNote g WHERE g.grnNumber LIKE :prefix%")
    String findMaxGrnNumber(@Param("prefix") String prefix);
}
