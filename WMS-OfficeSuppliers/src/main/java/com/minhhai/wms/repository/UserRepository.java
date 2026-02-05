package com.minhhai.wms.repository;

import com.minhhai.wms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u.passwordHash FROM User u WHERE u.username = :username")
    Optional<String> findPasswordHashByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.warehouse WHERE u.username = :username")
    Optional<User> findByUsernameWithWarehouse(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.warehouse ORDER BY u.username")
    List<User> findAllWithWarehouse();

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUserIdNot(String username, Integer userId);
}
