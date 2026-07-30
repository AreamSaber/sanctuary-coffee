package com.coffee;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 咖啡店系统启动类
 * 
 * @author Coffee Shop Team
 * @since 2024-11-18
 */
@SpringBootApplication
@MapperScan("com.coffee.mapper")
@EnableCaching
@EnableAsync
@EnableScheduling
public class CoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeApplication.class, args);
        System.out.println("========================================");
        System.out.println("   咖啡店系统启动成功！");
        System.out.println("   接口文档地址: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
