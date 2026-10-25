package org.kirpich.smart_profile.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ClockConfig {

    /** Через Clock считаем возраст бизнеса — в тестах его можно зафиксировать. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
