package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.repository.UserRepository;
import com.minhhai.wms.repository.WarehouseRepository;
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

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User save(com.minhhai.wms.dto.UserDTO userDTO) {
        // Uniqueness check
        if (userDTO.getUserId() == null) {
            if (userRepository.existsByUsername(userDTO.getUsername())) {
                throw new IllegalArgumentException("Username already exists.");
            }
        } else {
            if (userRepository.existsByUsernameAndUserIdNot(userDTO.getUsername(), userDTO.getUserId())) {
                throw new IllegalArgumentException("Username already exists.");
            }
        }

        Warehouse warehouse = null;
        if (userDTO.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(userDTO.getWarehouseId()).orElse(null);
        }

        User user;
        if (userDTO.getUserId() != null) {
            // Update
            user = userRepository.findById(userDTO.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDTO.getUserId()));

            user.setUsername(userDTO.getUsername());
            user.setFullName(userDTO.getFullName());
            user.setRole(userDTO.getRole());
            user.setWarehouse(warehouse);

            if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
                user.setPasswordHash(PasswordUtil.hashPassword(userDTO.getPassword()));
            }
        } else {
            // Create
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

        return userRepository.save(user);
    }

    @Override
    public void toggleActive(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (user.getIsActive()) {
            if ("System Admin".equals(user.getRole())) {
                long activeAdminCount = userRepository.countByRoleAndIsActiveTrue("System Admin");

                if (activeAdminCount <= 1) {
                    throw new IllegalArgumentException("Can not deactive the last admin in the system!");
                }
            }
        }
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String plainPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
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
        return userRepository.searchByKeyword(keyword.trim());
    }
}
