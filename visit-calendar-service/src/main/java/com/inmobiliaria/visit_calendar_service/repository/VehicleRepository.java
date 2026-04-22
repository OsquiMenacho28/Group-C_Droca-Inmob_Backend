package com.inmobiliaria.visit_calendar_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.inmobiliaria.visit_calendar_service.model.Vehicle;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
  Optional<Vehicle> findByLicensePlate(String licensePlate);
}
