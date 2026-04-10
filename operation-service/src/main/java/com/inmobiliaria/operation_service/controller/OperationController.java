package com.inmobiliaria.operation_service.controller;

import com.inmobiliaria.operation_service.model.Operation;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.ArrayList;


@RestController
@RequestMapping("/api/operations")
public class OperationController {

    @GetMapping
    public ResponseEntity<List<Operation>> getAllOperations() {
        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> getOperationById(@PathVariable String id) {
        return ResponseEntity.notFound().build();
    }
}