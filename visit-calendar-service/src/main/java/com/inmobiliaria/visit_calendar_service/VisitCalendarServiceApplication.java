package com.inmobiliaria.visit_calendar_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class VisitCalendarServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(VisitCalendarServiceApplication.class, args);
  }
}
