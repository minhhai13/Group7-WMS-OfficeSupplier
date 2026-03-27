package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.UserDao;
import com.minhhai.wms.dao.mapper.UserRowMapper;
import com.minhhai.wms.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private static final String BASE_SELECT = """
            SELECT u.UserID, u.Username, u.PasswordHash, u.FullName, u.Role, u.WarehouseID, u.IsActive,
                   w.WarehouseCode, w.WarehouseName, w.Address, w.IsActive AS WarehouseIsActive
            FROM Users u
            LEFT JOIN Warehouses w ON u.WarehouseID = w.WarehouseID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper rowMapper = new UserRowMapper();

    public UserDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> findAll() {
        return jdbcTemplate.query(BASE_SELECT, rowMapper);
    }

    @Override
    public Optional<User> findById(Integer id) {
        List<User> users = jdbcTemplate.query(BASE_SELECT + " WHERE u.UserID = ?", rowMapper, id);
        return users.stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(BASE_SELECT + " WHERE u.Username = ?", rowMapper, username);
        return users.stream().findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM Users WHERE Username = ?", Long.class, username);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByUsernameAndUserIdNot(String username, Integer userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Users WHERE Username = ? AND UserID <> ?",
                Long.class,
                username,
                userId
        );
        return count != null && count > 0;
    }

    @Override
    public List<User> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE u.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<User> searchByKeyword(String keyword) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE LOWER(u.FullName) LIKE ? OR LOWER(u.Username) LIKE ?",
                rowMapper,
                search,
                search
        );
    }

    @Override
    public long countByRoleAndIsActiveTrue(String role) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Users WHERE Role = ? AND IsActive = 1",
                Long.class,
                role
        );
        return count != null ? count : 0;
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM Users", Long.class);
        return count != null ? count : 0;
    }

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Users (Username, PasswordHash, FullName, Role, WarehouseID, IsActive) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getRole());
                if (user.getWarehouse() != null) {
                    ps.setInt(5, user.getWarehouse().getWarehouseId());
                } else {
                    ps.setObject(5, null);
                }
                ps.setBoolean(6, user.getIsActive() != null ? user.getIsActive() : true);
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                user.setUserId(keyHolder.getKey().intValue());
            }
            return user;
        }
        jdbcTemplate.update(
                "UPDATE Users SET Username = ?, PasswordHash = ?, FullName = ?, Role = ?, WarehouseID = ?, IsActive = ? WHERE UserID = ?",
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole(),
                user.getWarehouse() != null ? user.getWarehouse().getWarehouseId() : null,
                user.getIsActive(),
                user.getUserId()
        );
        return user;
    }
}
