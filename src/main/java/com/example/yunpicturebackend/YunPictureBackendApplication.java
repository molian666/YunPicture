package com.example.yunpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.example.yunpicturebackend.mapper")
public class YunPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunPictureBackendApplication.class, args);
    }

}
