package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    /*1. Spring Boot 启动，扫描到 MqConfig
    2. 执行 @Bean 方法，创建 Jackson2JsonMessageConverter 对象
    3. Spring 发现 RabbitTemplate 需要一个 MessageConverter
    4. 自动把 Jackson2JsonMessageConverter 注入到 RabbitTemplate
    5. 之后所有 rabbitTemplate.convertAndSend() 都会用 JSON 序列化*/

}
