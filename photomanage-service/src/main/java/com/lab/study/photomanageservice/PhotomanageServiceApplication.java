package com.lab.study.photomanageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PhotomanageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotomanageServiceApplication.class, args);
    }

}
