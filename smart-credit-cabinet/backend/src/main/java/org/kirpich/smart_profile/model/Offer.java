package org.kirpich.smart_profile.model;

import java.time.Instant;

import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferKind;

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

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public class Offer {

    public enum Status {
        ACTIVE,
        ACCEPTED,
        SIGNED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    private Long decisionId;

    @Enumerated(EnumType.STRING)
    private OfferKind kind;

    private String productCode;
    private long amount;
    private int termMonths;
    private double rate;
    private long monthlyPayment;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private String signingCodeHash;
    private Instant createdAt;
    private Instant acceptedAt;
    private Instant signedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
