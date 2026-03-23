package com.inmobiliaria.property_service.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "properties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyDocument extends BaseDocument {
    @Id
    private String id;
    private String title;
    private String address;
    private Double price;
    
    // Nuevos campos para la historia de usuario
    private String type;
    private Double m2;
    private Integer rooms;
    private String status; 
    
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private String assignedAgentId;

    @Builder.Default
    private List<AssignmentHistory> assignmentHistory = new ArrayList<>();
}