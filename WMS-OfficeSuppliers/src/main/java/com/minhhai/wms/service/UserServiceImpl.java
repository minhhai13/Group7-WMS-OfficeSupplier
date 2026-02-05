package com.minhhai.wms.service;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.repository.UserRepository;
import com.minhhai.wms.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final List<String> ROLE_OPTIONS = List.of(
            "System Admin",
            "Warehouse Admin",
            "Warehouse Manager",
            "Purchasing Manager",
            "Purchasing Staff",
            "Sales Manager",
            "Sales Staff",
            "Storekeeper"
    );

    private static final List<String> WAREHOUSE_REQUIRED_ROLES = List.of(
            "Warehouse Admin",
            "Warehouse Manager",
            "Storekeeper"
    );

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllWithWarehouse();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findPasswordHashByUsername(String username) {
        return userRepository.findPasswordHashByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameWithWarehouse(String username) {
        return userRepository.findByUsernameWithWarehouse(username);
    }

    @Override
    @Transactional
    public User save(User user, String plainPassword) {
        if (plainPassword != null && !plainPassword.isBlank()) {
            user.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        } else if (user.getUserId() != null) {
            userRepository.findById(user.getUserId())
                    .ifPresent(existing -> user.setPasswordHash(existing.getPasswordHash()));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsernameExcludingId(String username, Integer excludeUserId) {
        return userRepository.existsByUsernameAndUserIdNot(username, excludeUserId);
    }

    @Override
    public List<String> getRoleOptions() {
        return ROLE_OPTIONS;
    }

    @Override
    public List<String> getWarehouseRequiredRoles() {
        return WAREHOUSE_REQUIRED_ROLES;
    }
}
