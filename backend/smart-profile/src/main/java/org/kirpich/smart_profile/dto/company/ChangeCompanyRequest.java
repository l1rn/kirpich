package org.kirpich.smart_profile.dto.company;

import lombok.Data;

@Data
public class ChangeCompanyRequest {
    private Integer id;
    private String name;
    private String inn;
    private String ogrn;
    private String legalAddress;
    private Long revenue;
}
