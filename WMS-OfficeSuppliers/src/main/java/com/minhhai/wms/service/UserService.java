package com.minhhai.wms.service;

import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    Optional<User> findById(Integer id);

    Optional<String> findPasswordHashByUsername(String username);

    Optional<User> findByUsernameWithWarehouse(String username);

    User save(User user, String plainPassword);

    void deleteById(Integer id);

    boolean existsByUsername(String username);

    boolean existsByUsernameExcludingId(String username, Integer excludeUserId);

    List<String> getRoleOptions();

    List<String> getWarehouseRequiredRoles();
}
