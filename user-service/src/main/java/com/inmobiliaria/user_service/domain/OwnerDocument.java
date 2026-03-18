package com.inmobiliaria.user_service.domain;

import lombok.*;
import org.springframework.data.annotation.TypeAlias;
import java.util.List;

@TypeAlias("owner")
@Getter
@Setter
@NoArgsConstructor
public class OwnerDocument extends PersonDocument {
    private String taxId;
    private List<String> propertyIds;

    @Builder
    public OwnerDocument(String id, String authUserId, String firstName, String lastName, String fullName, 
                         java.time.LocalDate birthDate, String phone, String email, 
                         java.util.List<String> roleIds, boolean customRole,
                         String taxId, List<String> propertyIds) {
        super(id, authUserId, firstName, lastName, fullName, birthDate, phone, email, PersonType.OWNER, roleIds, customRole);
        this.taxId = taxId;
        this.propertyIds = propertyIds;
    }
}
