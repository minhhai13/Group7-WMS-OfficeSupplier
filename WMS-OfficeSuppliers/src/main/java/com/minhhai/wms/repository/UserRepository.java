package com.minhhai.wms.repository;

import com.minhhai.wms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUserIdNot(String username, Integer userId);

    List<User> findByWarehouseWarehouseId(Integer warehouseId);

    List<User> findByRole(String role);
}
