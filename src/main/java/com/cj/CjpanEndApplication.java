package com.cj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
// 事务生效
@EnableTransactionManagement
// 开启任务调度 定时任务
@EnableScheduling
// 异步调用
@EnableAsync
@MapperScan("com.cj.mappers")
public class CjpanEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(CjpanEndApplication.class, args);
    }

}
