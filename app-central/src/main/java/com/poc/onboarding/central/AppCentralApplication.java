package com.poc.onboarding.central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.poc.onboarding.central",
        "com.poc.onboarding.common"
})
public class AppCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppCentralApplication.class, args);
    }
}
