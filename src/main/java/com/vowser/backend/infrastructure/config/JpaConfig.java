package com.vowser.backend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * createdAt, updatedAt 자동 관리를 위한 설정
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}