package com.snoozeshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the SnoozeShare backend. */
@SpringBootApplication
public class SnoozeShareApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SnoozeShareApplication.class, args);
    }
}
