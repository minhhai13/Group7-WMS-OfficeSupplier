package com.minhhai.wms.repository;

import com.minhhai.wms.entity.TransferNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferNoteRepository extends JpaRepository<TransferNote, Integer> {
    
    @Query("SELECT tn FROM TransferNote tn WHERE tn.warehouse.warehouseId = :warehouseId")
    List<TransferNote> findByWarehouse_WarehouseId(@Param("warehouseId") Integer warehouseId);
    
    @Query("SELECT MAX(tn.tnNumber) FROM TransferNote tn WHERE tn.tnNumber LIKE :prefix%")
    String findMaxTnNumber(@Param("prefix") String prefix);
}
