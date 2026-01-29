package com.poc.onboarding.ldap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.poc.onboarding.ldap",
        "com.poc.onboarding.common"
})
public class ServiceLdapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceLdapApplication.class, args);
    }
}
