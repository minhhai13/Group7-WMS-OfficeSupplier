package com.minhhai.wms.service;

import com.minhhai.wms.dto.UserDTO;
import com.minhhai.wms.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    Optional<User> findById(Integer id);

    User save(User user);

    void toggleActive(Integer userId);

    User save(UserDTO userDTO);

    Optional<User> authenticate(String username, String plainPassword);

    List<User> search(String keyword);
    Page<User> findPaginated(String keyword, int page, int size);
}
