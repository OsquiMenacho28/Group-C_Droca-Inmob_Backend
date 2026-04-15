package com.inmobiliaria.user_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.inmobiliaria.user_service.domain.FavoriteHistoryDocument;

public interface FavoriteHistoryRepository
    extends MongoRepository<FavoriteHistoryDocument, String> {
  List<FavoriteHistoryDocument> findByAuthUserIdOrderByTimestampDesc(
      String authUserId, Pageable pageable);

  List<FavoriteHistoryDocument> findByAuthUserIdAndPropertyIdOrderByTimestampDesc(
      String authUserId, String propertyId);
}
