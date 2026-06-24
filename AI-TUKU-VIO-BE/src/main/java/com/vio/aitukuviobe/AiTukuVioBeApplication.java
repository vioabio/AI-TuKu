package com.vio.aitukuviobe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.vio.aitukuviobe.mapper")
//代理暴露，代理类
@EnableAspectJAutoProxy(exposeProxy = true)
public class
AiTukuVioBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTukuVioBeApplication.class, args);
    }

}
