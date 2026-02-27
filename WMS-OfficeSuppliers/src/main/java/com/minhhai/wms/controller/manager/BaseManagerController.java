package com.minhhai.wms.controller.manager;

import com.minhhai.wms.entity.User;
import jakarta.servlet.http.HttpSession;

public abstract class BaseManagerController {

    protected Integer getManagerWarehouseId(HttpSession session) {
        // ĐỔI "currentUser" THÀNH "loggedInUser" CHO KHỚP VỚI LOGIN CONTROLLER
        User currentUser = (User) session.getAttribute("loggedInUser");

        if (currentUser == null) {
            throw new SecurityException("Vui lòng đăng nhập.");
        }

        if (!"Warehouse Manager".equals(currentUser.getRole())) {
            throw new SecurityException("Truy cập bị từ chối: Dành riêng cho Warehouse Manager.");
        }

        if (currentUser.getWarehouse() == null) {
            throw new IllegalStateException("Tài khoản chưa được phân công quản lý kho.");
        }

        return currentUser.getWarehouse().getWarehouseId();
    }
}