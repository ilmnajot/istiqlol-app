package org.example.moliyaapp.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.example.moliyaapp.utils.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@EnableJpaAuditing(auditorAwareRef = "hibernateConfig")
@Configuration
public class HibernateConfig implements AuditorAware<Long> {

    private static final Logger log = LoggerFactory.getLogger(HibernateConfig.class);

    private final UserSession userSession;

    public HibernateConfig(UserSession userSession) {
        this.userSession = userSession;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Hibernate successfully");
    }

    @Nonnull
    @Override
    public Optional<Long> getCurrentAuditor() {

        Long userId = userSession.getUser();

        log.info("Current auditor: {}", userId);
        System.out.println("Auditing Current User ID = " + userId);
        return Optional.ofNullable(userId);
    }
}
