package org.kirpich.smart_profile.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки приложения (раздел app в application.yaml). Секреты приходят из переменных окружения. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Registry registry, Pipeline pipeline, String uploadDir, List<String> corsOrigins) {

    public record Jwt(String secret, Duration ttl) {
    }

    /** Внешние источники (ЕГРЮЛ, ФНС, БКИ); в демо — WireMock. */
    public record Registry(String baseUrl, Duration timeout, int maxAttempts) {
    }

    /** Конвейер: пауза между шагами, чтобы на демо было видно движение заявки. */
    public record Pipeline(boolean enabled, long stepDelayMs) {
    }
}
