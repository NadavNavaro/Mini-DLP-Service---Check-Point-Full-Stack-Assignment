package com.nadav.dlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Bootstraps the single backend application and enables Spring component discovery. */
@SpringBootApplication
public class DlpApplication {
    public static void main(String[] args) {
        SpringApplication.run(DlpApplication.class, args);
    }
}
