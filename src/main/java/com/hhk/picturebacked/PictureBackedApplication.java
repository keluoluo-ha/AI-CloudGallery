package com.hhk.picturebacked;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.hhk.picturebacked.mapper")
public class PictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackedApplication.class, args);
    }

}
