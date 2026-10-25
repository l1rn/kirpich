package org.kirpich.smart_profile.model;

import java.time.Instant;

import org.kirpich.smart_profile.domain.Actor;
import org.kirpich.smart_profile.domain.ApplicationStatus;

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

/** Запрос дополнительной информации у клиента — от системы или от андеррайтера. */
@Entity
@Table(name = "info_requests")
@Getter
@Setter
@NoArgsConstructor
public class InfoRequest {

    public enum Status {
        OPEN,
        ANSWERED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Enumerated(EnumType.STRING)
    private Actor requestedBy;

    private Long requestedByUser;

    /** Куда вернуть заявку после ответа: CHECKING (запросила система) или MANUAL_REVIEW (андеррайтер). */
    @Enumerated(EnumType.STRING)
    private ApplicationStatus returnStatus;

    /** JSON: пункты запроса [{key, label, type: TEXT|FILE}]. */
    private String items;
    private String note;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    /** JSON: ответы клиента на текстовые пункты. */
    private String response;
    private Instant createdAt;
    private Instant answeredAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
