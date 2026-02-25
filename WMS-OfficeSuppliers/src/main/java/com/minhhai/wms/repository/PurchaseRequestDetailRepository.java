package com.minhhai.wms.repository;

import com.minhhai.wms.entity.PurchaseRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestDetailRepository extends JpaRepository<PurchaseRequestDetail, Integer> {

    List<PurchaseRequestDetail> findByPurchaseRequest_PrId(Integer prId);
}
