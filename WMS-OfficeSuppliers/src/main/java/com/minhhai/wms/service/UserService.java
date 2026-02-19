package com.minhhai.wms.service;

import com.minhhai.wms.dto.UserDTO;
import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    Optional<User> findById(Integer id);

    Optional<User> findByUsername(String username);

    List<User> findByWarehouseId(Integer warehouseId);

    User save(User user);

    void toggleActive(Integer userId);

    boolean existsByUsername(String username);

    boolean existsByUsernameExcluding(String username, Integer userId);

    User save(UserDTO userDTO);

    Optional<User> authenticate(String username, String plainPassword);
}
