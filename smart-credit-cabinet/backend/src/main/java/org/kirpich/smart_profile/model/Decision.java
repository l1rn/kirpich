package org.kirpich.smart_profile.model;

import java.time.Instant;

import org.kirpich.smart_profile.domain.decision.DecisionEngine.Outcome;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Решение по заявке — движка (ENGINE) или андеррайтера (UNDERWRITER) — вместе с объяснением. */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

    public static final String ENGINE = "ENGINE";
    public static final String UNDERWRITER = "UNDERWRITER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    private String source;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    private Integer score;
    private String grade;
    /** JSON: список факторов скоринга с баллами. */
    private String factors;
    /** JSON: стоп-факторы. */
    private String stopFactors;
    /** JSON: причины решения человеческим языком. */
    private String reasons;
    /** JSON: предложение клиенту или рекомендация андеррайтеру. */
    private String proposal;
    private String note;
    private Long decidedBy;
    private String policyVersion;
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
