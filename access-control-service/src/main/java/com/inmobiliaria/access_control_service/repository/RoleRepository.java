package com.inmobiliaria.access_control_service.repository;

import com.inmobiliaria.access_control_service.domain.RoleDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<RoleDocument, String> {

  Optional<RoleDocument> findByCode(String code);

  boolean existsByCode(String code);

  boolean existsByName(String name);
}
