package com.inmobiliaria.user_service.repository;

import com.inmobiliaria.user_service.domain.ClientInteractionDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientInteractionRepository
    extends MongoRepository<ClientInteractionDocument, String> {

  boolean existsByReferenceId(String referenceId);

  Optional<ClientInteractionDocument> findByReferenceId(String referenceId);
}
