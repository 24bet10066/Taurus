package com.serviceos.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.serviceos.job.feign")
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
