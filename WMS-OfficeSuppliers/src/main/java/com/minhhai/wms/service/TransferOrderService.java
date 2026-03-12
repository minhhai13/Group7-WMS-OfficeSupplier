package com.minhhai.wms.service;

import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.User;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface TransferOrderService {
    
    TransferOrderDTO getTOById(Integer id);

    TransferOrderDTO submitTO(@Valid TransferOrderDTO toDTO, User user);

    void saveDraftTO(@Valid TransferOrderDTO toDTO, User user);

    void deleteTO(Integer id, User user);

    List<TransferOrderDTO> getOutgoingTransferOrders(Integer warehouseId, String status, Integer sourceWarehouseId);

    List<TransferOrderDTO> getIncomingWarehouseTransferOrders(Integer warehouseId, String filterStatus, Integer destWarehouseId);

    void approveTO(Integer id, User user);

    void rejectTO(Integer id, String trim);

    List<Map<String, String>> getAvailableUoMs(Integer productId);
}