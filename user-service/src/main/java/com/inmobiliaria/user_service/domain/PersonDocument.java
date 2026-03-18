package com.inmobiliaria.user_service.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Document(collection = "persons")
@TypeAlias("person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class PersonDocument extends BaseDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String authUserId;

    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private String phone;
    private String email;

    private PersonType personType;

    private List<String> roleIds;
    private boolean customRole;
}
