package com.serviceos.technician;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.serviceos.technician.feign")
public class TechnicianServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicianServiceApplication.class, args);
    }
}
