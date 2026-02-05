package com.minhhai.wms.config;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.repository.UserRepository;
import com.minhhai.wms.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123";
    private static final String DEFAULT_ADMIN_ROLE = "System Admin";
    private static final String DEFAULT_ADMIN_FULL_NAME = "System Administrator";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = User.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .passwordHash(PasswordUtil.hashPassword(DEFAULT_ADMIN_PASSWORD))
                .fullName(DEFAULT_ADMIN_FULL_NAME)
                .role(DEFAULT_ADMIN_ROLE)
                .warehouse(null)
                .build();
        userRepository.save(admin);
    }
}
