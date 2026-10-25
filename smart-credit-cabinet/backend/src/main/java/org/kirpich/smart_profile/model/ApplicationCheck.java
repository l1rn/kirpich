package org.kirpich.smart_profile.model;

import java.time.Instant;

import org.kirpich.smart_profile.domain.CheckStatus;
import org.kirpich.smart_profile.domain.PipelineStep;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Шаг конвейера внутри статуса CHECKING. result — JSON-снимок ответа источника. */
@Entity
@Table(name = "application_checks")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Enumerated(EnumType.STRING)
    private PipelineStep step;

    private int stepOrder;

    @Enumerated(EnumType.STRING)
    private CheckStatus status;

    private int attempts;
    private String result;
    private String error;
    private Instant updatedAt;

    public ApplicationCheck(CreditApplication application, PipelineStep step, CheckStatus status) {
        this.application = application;
        this.step = step;
        this.stepOrder = step.ordinal();
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
