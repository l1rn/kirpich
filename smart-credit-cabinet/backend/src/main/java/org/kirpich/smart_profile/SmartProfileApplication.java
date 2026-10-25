package org.kirpich.smart_profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartProfileApplication.class, args);
    }
}
