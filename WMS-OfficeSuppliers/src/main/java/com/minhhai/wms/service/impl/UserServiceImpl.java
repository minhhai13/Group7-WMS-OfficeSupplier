package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.UserDao;
import com.minhhai.wms.dao.WarehouseDao;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.UserService;
import com.minhhai.wms.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final WarehouseDao warehouseDao;

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        return userDao.findById(id);
    }

    @Override
    public User save(User user) {
        return userDao.save(user);
    }

    @Override
    public User save(com.minhhai.wms.dto.UserDTO userDTO) {
        // Uniqueness check
        if (userDTO.getUserId() == null) {
            if (userDao.existsByUsername(userDTO.getUsername())) {
                throw new IllegalArgumentException("Username already exists.");
            }
        } else {
            if (userDao.existsByUsernameAndUserIdNot(userDTO.getUsername(), userDTO.getUserId())) {
                throw new IllegalArgumentException("Username already exists.");
            }
        }

        Warehouse warehouse = null;
        if (userDTO.getWarehouseId() != null) {
            warehouse = warehouseDao.findById(userDTO.getWarehouseId()).orElse(null);
        }

        User user;
        if (userDTO.getUserId() != null) {
            user = userDao.findById(userDTO.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDTO.getUserId()));
            if (user.getIsActive() && "System Admin".equals(user.getRole()) && !"System Admin".equals(userDTO.getRole())) {
                long activeAdminCount = userDao.countByRoleAndIsActiveTrue("System Admin");

                if (activeAdminCount <= 1) {
                    throw new IllegalArgumentException("Can not change role of the last admin in the system!");
                }
            }
            user.setUsername(userDTO.getUsername());
            user.setFullName(userDTO.getFullName());
            user.setRole(userDTO.getRole());
            user.setWarehouse(warehouse);

            if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
                user.setPasswordHash(PasswordUtil.hashPassword(userDTO.getPassword()));
            }
        } else {
            if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required for new users.");
            }
            user = User.builder()
                    .username(userDTO.getUsername())
                    .passwordHash(PasswordUtil.hashPassword(userDTO.getPassword()))
                    .fullName(userDTO.getFullName())
                    .role(userDTO.getRole())
                    .warehouse(warehouse)
                    .isActive(true)
                    .build();
        }

        return userDao.save(user);
    }

    @Override
    public void toggleActive(Integer userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            if (user.getIsActive() && "System Admin".equals(user.getRole())) {
                long activeAdminCount = userDao.countByRoleAndIsActiveTrue("System Admin");

                if (activeAdminCount <= 1) {
                    throw new IllegalArgumentException("Can not deactive the last admin in the system!");
                }
            }
        user.setIsActive(!user.getIsActive());
        userDao.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String plainPassword) {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getIsActive())
                    && PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return userDao.searchByKeyword(keyword.trim());
    }
}
