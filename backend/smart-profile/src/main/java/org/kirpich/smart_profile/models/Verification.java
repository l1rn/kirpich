package org.kirpich.smart_profile.models;

import jakarta.persistence.*;
import lombok.*;
import org.kirpich.smart_profile.enums.BkiStatus;
import org.kirpich.smart_profile.enums.EgrulStatus;
import org.kirpich.smart_profile.enums.FinalVerdict;

import java.time.LocalDateTime;

@Entity
@Table(name="verifications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Verification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private EgrulStatus egrulStatus;

    @Enumerated(EnumType.STRING)
    private BkiStatus bkiStatus;

    @Enumerated(EnumType.STRING)
    private FinalVerdict finalVerdict;

    private LocalDateTime issueDate;
    @Column(length = 1000)
    private String reasons;
}
