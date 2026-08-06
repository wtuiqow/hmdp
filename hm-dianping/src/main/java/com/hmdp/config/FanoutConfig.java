package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FanoutConfig {

    //hmdp.mq
    @Bean
    public FanoutExchange hmdq_fanoutExchange() {
        return new FanoutExchange("hmdp.mq");
    }

    //hmdp.queue1
    @Bean
    public Queue hmdpQueue1() {
        return new Queue("hmdp.queue1");
    }

    //绑定队列到交换机
    @Bean
    public Binding hmdpfanoutBinding1(Queue hmdpQueue1,FanoutExchange hmdq_fanoutExchange) {
        return BindingBuilder.bind(hmdpQueue1).to(hmdq_fanoutExchange);
    }

}

//    @Bean
//    public Queue fanoutQueue2() {           // ← Bean 名称：fanoutQueue2
//        return new Queue("fanout.queue2"); // ← RabbitMQ 队列名称：fanout.queue2
//    }
//    @Bean
//    public Binding fanoutBinding2(Queue fanoutQueue2, FanoutExchange fanoutExchange) {
//    // fanoutQueue2 → Spring 根据参数名查找 Bean
//    // 找到名为 fanoutQueue2 的 Bean（不是 "fanout.queue2"）
//}
