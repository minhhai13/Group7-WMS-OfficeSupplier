package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Integer> {
    //tim tr theo
    Optional<Transfer> findById(Integer id);

    //pov: mình là source, ben kia la dest, source nhan tr tu dest de xuat hang
    List<Transfer> findBySourceWarehouse_WarehouseId(Integer warehouseId);

    List<Transfer> findBySourceWarehouse_WarehouseIdAndTransferStatus(
            Integer warehouseId, String transferStatus);

    // pov: minh la dest, ben kia la source, dest gui tr cho source de lay hang
    List<Transfer> findByDestinationWarehouse_WarehouseId(Integer warehouseId);

    List<Transfer> findByDestinationWarehouse_WarehouseIdAndTransferStatus(
            Integer warehouseId, String transferStatus);

    // cả 2
    List<Transfer> findBySourceWarehouse_WarehouseIdAndTransferStatusAndDestinationWarehouse_WarehouseId(
            Integer sourceId, String transferStatus, Integer destId);
    List<Transfer> findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(
            Integer sourceId, Integer destId);

    @Query("SELECT MAX(trs.transferNumber) FROM Transfer trs WHERE trs.transferNumber LIKE :prefix%")
    String findMaxTransferNumber(@Param("prefix") String prefix);
}
