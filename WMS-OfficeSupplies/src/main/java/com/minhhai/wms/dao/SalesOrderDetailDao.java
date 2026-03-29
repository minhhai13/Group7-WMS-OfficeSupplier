package com.minhhai.wms.dao;

import com.minhhai.wms.entity.SalesOrderDetail;

import java.util.List;

public interface SalesOrderDetailDao {

    List<SalesOrderDetail> findBySoId(Integer soId);

    SalesOrderDetail save(SalesOrderDetail detail);

    void deleteBySoId(Integer soId);

    void updateIssuedQty(Integer soDetailId, Integer issuedQty);
}
