package com.inmobiliaria.property_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inmobiliaria.property_service.domain.OperationType;
import com.inmobiliaria.property_service.dto.request.PropertyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PropertyController Unit Tests")
class PropertyControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  private PropertyRequest testPropertyRequest;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    testPropertyRequest =
        new PropertyRequest(
            "Test Property",
            "123 Test St",
            "Downtown",
            100000.0,
            "Apartment",
            OperationType.VENTA,
            100.0,
            3,
            java.util.Set.of(),
            null);
  }

  // Note: These tests primarily check for authentication and endpoint accessibility.
  // More comprehensive tests would require mocking the service layer and testing responses.

  @Test
  @DisplayName("GET /properties should return list of properties")
  void testFindAllProperties() throws Exception {
    mockMvc
        .perform(get("/properties").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /properties/{id} should return property by id")
  void testFindPropertyById() throws Exception {
    mockMvc
        .perform(get("/properties/test-id").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /properties/agent/{agentId} should return agent's properties")
  void testFindPropertiesByAgent() throws Exception {
    mockMvc
        .perform(get("/properties/agent/agent-123").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /properties requires authentication")
  void testCreatePropertyRequiresAuth() throws Exception {
    mockMvc
        .perform(
            post("/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPropertyRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /properties/owner/{ownerId} should return owner's properties")
  void testFindPropertiesByOwner() throws Exception {
    mockMvc
        .perform(get("/properties/owner/owner-123").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /properties/{id}/status-history requires authentication")
  void testGetStatusHistoryRequiresAuth() throws Exception {
    mockMvc
        .perform(get("/properties/test-id/status-history").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /properties/{id}/responsable should return responsable info")
  void testGetResponsable() throws Exception {
    mockMvc
        .perform(get("/properties/test-id/responsable").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
