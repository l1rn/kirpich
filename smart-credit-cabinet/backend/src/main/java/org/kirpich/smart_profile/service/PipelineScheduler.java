package org.kirpich.smart_profile.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Фоновый запуск конвейера. Пауза между тиками = пауза между шагами заявки, чтобы на демо
 * было видно, как она проходит проверки. В тестах выключен (app.pipeline.enabled=false).
 */
@Component
@ConditionalOnProperty(prefix = "app.pipeline", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PipelineScheduler {

    private final PipelineService pipeline;

    public PipelineScheduler(PipelineService pipeline) {
        this.pipeline = pipeline;
    }

    @Scheduled(fixedDelayString = "${app.pipeline.step-delay-ms}")
    public void tick() {
        pipeline.processTick();
    }
}
