package org.kirpich.smart_profile.dto.application;

import lombok.Data;

@Data
public class CreateApplicationRequest {
//    private Integer companyId;
    private Integer creditProductId;
    private Long requestedAmount;
    private Integer termMonths;
}
