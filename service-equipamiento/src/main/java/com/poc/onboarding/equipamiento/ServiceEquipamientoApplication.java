package com.poc.onboarding.equip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.poc.onboarding.equip",
        "com.poc.onboarding.common"
})
public class ServiceEquipamientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceEquipamientoApplication.class, args);
    }
}
