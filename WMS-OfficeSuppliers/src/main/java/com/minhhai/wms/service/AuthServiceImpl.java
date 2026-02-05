package com.minhhai.wms.service;

import com.minhhai.wms.dto.LoginRequest;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    public static final String SESSION_USER = "currentUser";

    private final UserService userService;

    @Override
    public Optional<User> login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return Optional.empty();
        }
        String username = request.getUsername().trim();
        String password = request.getPassword();

        Optional<String> hashOpt = userService.findPasswordHashByUsername(username);
        if (hashOpt.isEmpty() || !PasswordUtil.verifyPassword(password, hashOpt.get())) {
            return Optional.empty();
        }
        return userService.findByUsernameWithWarehouse(username);
    }

    @Override
    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}
