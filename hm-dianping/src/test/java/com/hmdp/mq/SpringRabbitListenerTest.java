package com.hmdp.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SpringRabbitListenerTest {

    @RabbitListener(queues = "fanout.queue1")
    public void listenFanoutQueue_1(String msg) throws InterruptedException {
        System.out.println("消费者_1 接收到 fanout.queue1 的消息：" + msg + LocalTime.now());
    }

}
