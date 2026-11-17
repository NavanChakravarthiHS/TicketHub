package com.tickethub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. @SpringBootApplication turns on component scanning, auto-configuration,
 * and registers this class as a source of beans. Scanning starts in com.tickethub,
 * so every class in config, controller, service, and the other packages is picked up.
 */
@SpringBootApplication
public class TicketHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketHubApplication.class, args);
    }
}
