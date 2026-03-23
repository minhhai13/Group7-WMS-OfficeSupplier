package com.minhhai.wms.repository;

import com.minhhai.wms.entity.TransferOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferOrderRepository extends JpaRepository<TransferOrder, Integer> {

    Optional<TransferOrder> findById(Integer id);

    List<TransferOrder> findBySourceWarehouse_WarehouseId(Integer warehouseId);

    List<TransferOrder> findBySourceWarehouse_WarehouseIdAndStatus(Integer warehouseId, String status);

    List<TransferOrder> findByDestinationWarehouse_WarehouseId(Integer warehouseId);

    List<TransferOrder> findByDestinationWarehouse_WarehouseIdAndStatus(Integer warehouseId, String status);

    List<TransferOrder> findBySourceWarehouse_WarehouseIdAndStatusAndDestinationWarehouse_WarehouseId(Integer sourceId, String status, Integer destId);

    List<TransferOrder> findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(Integer sourceId, Integer destId);

    @Query("SELECT MAX(t.toNumber) FROM TransferOrder t WHERE t.toNumber LIKE :prefix%")
    String findMaxToNumber(@Param("prefix") String prefix);
}