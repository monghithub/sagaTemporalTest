package com.poc.onboarding.sistemas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.poc.onboarding.sistemas",
        "com.poc.onboarding.common"
})
public class ServiceSistemasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceSistemasApplication.class, args);
    }
}
