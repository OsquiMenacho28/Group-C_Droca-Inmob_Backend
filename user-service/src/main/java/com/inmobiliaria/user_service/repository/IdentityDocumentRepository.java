package com.inmobiliaria.user_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.inmobiliaria.user_service.domain.IdentityDocument;

public interface IdentityDocumentRepository extends MongoRepository<IdentityDocument, String> {

  List<IdentityDocument> findByPersonId(String personId);
}
