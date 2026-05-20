package com.inmobiliaria.visit_calendar_service;

import com.inmobiliaria.visit_calendar_service.config.ServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(ServiceProperties.class)
public class VisitCalendarServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(VisitCalendarServiceApplication.class, args);
  }
}
