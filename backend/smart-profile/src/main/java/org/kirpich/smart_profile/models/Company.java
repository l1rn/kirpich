package org.kirpich.smart_profile.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String inn;
    private String ogrn;
    private String legalAddress;
    private Long revenue;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User owner;
}
