package com.example.finance.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class SwaggerConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void openSwagger() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(
                        new URI("http://localhost:8080/swagger-ui/index.html")
                );
            }
        } catch (Exception e) {
            System.out.println("Could not open Swagger UI: " + e.getMessage());
        }
    }
}