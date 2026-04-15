package com.inmobiliaria.operation_service.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inmobiliaria.operation_service.dto.response.ApiResponse;
import com.inmobiliaria.operation_service.dto.response.ResponseFactory;
import com.inmobiliaria.operation_service.model.Operation;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

  private final ResponseFactory responseFactory;

  @GetMapping
  public ResponseEntity<ApiResponse<List<Operation>>> getAllOperations() {
    return ResponseEntity.ok(
        responseFactory.success("Operations retrieved successfully", new ArrayList<>()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Operation>> getOperationById(@PathVariable String id) {
    throw new com.inmobiliaria.operation_service.exception.ResourceNotFoundException(
        "Operation with ID " + id + " not found");
  }
}
