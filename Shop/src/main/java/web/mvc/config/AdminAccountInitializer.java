package web.mvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import web.mvc.domain.User;
import web.mvc.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.user-id:}")
    private String adminUserId;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.user-name:Administrator}")
    private String adminUserName;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminUserId) || !StringUtils.hasText(adminPassword)) {
            log.info("Admin account initialization skipped. ADMIN_USER_ID and ADMIN_PASSWORD are not configured.");
            return;
        }

        if (userRepository.existsByUserId(adminUserId)) {
            log.info("Admin account initialization skipped. User already exists: {}", adminUserId);
            return;
        }

        User admin = User.builder()
                .userId(adminUserId)
                .password(passwordEncoder.encode(adminPassword))
                .userName(adminUserName)
                .role("ROLE_ADMIN")
                .build();

        userRepository.save(admin);
        log.info("Admin account initialized: {}", adminUserId);
    }
}
