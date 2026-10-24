package org.kirpich.smart_profile.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.kirpich.smart_profile.enums.ApplicationStatus;

import java.time.LocalDateTime;


@Table(name="applications")
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "verification_id")
    private Verification verification;

    @ManyToOne
    @JoinColumn(name = "credit_product_id")
    private CreditProduct creditProduct;

    private Long requestedAmount;
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
