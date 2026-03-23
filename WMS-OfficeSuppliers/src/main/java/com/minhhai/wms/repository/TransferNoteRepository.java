package com.minhhai.wms.repository;

import com.minhhai.wms.entity.TransferNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferNoteRepository extends JpaRepository<TransferNote, Integer> {

    @Query("SELECT tn FROM TransferNote tn WHERE tn.warehouse.warehouseId = :warehouseId")
    List<TransferNote> findByWarehouse_WarehouseId(@Param("warehouseId") Integer warehouseId);

    @Query("SELECT MAX(tn.tnNumber) FROM TransferNote tn WHERE tn.tnNumber LIKE :prefix%")
    String findMaxTnNumber(@Param("prefix") String prefix);

    @Query("SELECT tn FROM TransferNote tn " +
           "LEFT JOIN FETCH tn.details d " +
           "LEFT JOIN FETCH d.product " +
           "LEFT JOIN FETCH d.fromBin " +
           "LEFT JOIN FETCH d.toBin " +
           "WHERE tn.tnId = :tnId")
    Optional<TransferNote> findByIdWithDetails(@Param("tnId") Integer tnId);
}
