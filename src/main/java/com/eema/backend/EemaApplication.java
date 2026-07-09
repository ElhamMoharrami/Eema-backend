package com.eema.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EemaApplication.class, args);
    }
}