package com.shiguangji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan({"com.shiguangji.common.mapper", "com.shiguangji.modules"})
@SpringBootApplication
public class ShiguangjiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiguangjiApplication.class, args);
    }
}
