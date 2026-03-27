package com.minhhai.wms.service;

import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.User;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface TransferOrderService {

    TransferOrderDTO getTOById(Integer id);

    void saveDraftTO(@Valid TransferOrderDTO trDTO, User user);

    TransferOrderDTO submitTO(@Valid TransferOrderDTO trDTO, User user);

    void deleteTO(Integer id, User user);

    //out going: mình là destination wh, gửi tr cho các wh khác
    //in coming: mình là source wh, duyệt tr từ các wh khác gửi
    List<TransferOrderDTO> getOutgoingWHTransferOrders(Integer warehouseId, Integer sourceWarehouseId, String status);

    List<TransferOrderDTO> getIncomingWHTransferOrders(Integer destWarehouseId, Integer warehouseId, String status);

    void approveTO(Integer id, User user);

    void rejectTO(Integer id, User user, String trim);
}