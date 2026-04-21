package com.inmobiliaria.user_service.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.inmobiliaria.user_service.domain.DocumentType;
import com.inmobiliaria.user_service.domain.IdentityDocument;
import com.inmobiliaria.user_service.repository.IdentityDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentityDocumentService {

  private final IdentityDocumentRepository repository;
  private final MinioStorageService storageService;

  public IdentityDocument uploadDocument(
      String personId, DocumentType documentType, MultipartFile file) {

    String fileKey = storageService.uploadFile(file, personId);
    String currentUserId =
        (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    IdentityDocument document =
        IdentityDocument.builder()
            .personId(personId)
            .documentType(documentType)
            .fileUrl(fileKey)
            .uploadDate(Instant.now())
            .uploadedById(currentUserId)
            .build();

    document.setCreatedAt(Instant.now());
    document.setCreatedBy(currentUserId);

    return repository.save(document);
  }

  public List<IdentityDocument> getDocumentsByPersonId(String personId) {
    List<IdentityDocument> documents = repository.findByPersonId(personId);
    documents.forEach(doc -> doc.setFileUrl(storageService.generatePresignedUrl(doc.getFileUrl())));
    return documents;
  }
}
