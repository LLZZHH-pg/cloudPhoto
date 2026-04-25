package com.lab.study.albummanageservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.lab.study.albummanageservice.mapper")
public class AlbumManageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlbumManageServiceApplication.class, args);
    }
}
