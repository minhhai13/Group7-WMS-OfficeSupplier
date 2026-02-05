package com.minhhai.wms.service;

import com.minhhai.wms.dto.LoginRequest;
import com.minhhai.wms.entity.User;

import java.util.Optional;

public interface AuthService {

    /**
     * Xác thực đăng nhập từ request. Chỉ khi xác thực thành công mới truy vấn User đầy đủ (JOIN FETCH warehouse) để trả về lưu session.
     */
    Optional<User> login(LoginRequest request);

    void logout(jakarta.servlet.http.HttpSession session);
}
