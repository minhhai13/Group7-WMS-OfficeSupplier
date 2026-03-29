package com.minhhai.wms.dao;

import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    List<User> findAll();

    Optional<User> findById(Integer id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUserIdNot(String username, Integer userId);

    List<User> findByWarehouseId(Integer warehouseId);

    List<User> searchByKeyword(String keyword);

    long countByRoleAndIsActiveTrue(String role);

    long count();

    User save(User user);
}
