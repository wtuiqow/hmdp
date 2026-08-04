package com.hmdp.mq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SpringAmqpTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendFanoutExchange() throws InterruptedException {
        //交换机名
        String exchangeName = "itcast.fanout";
        //消息
        String message = "Hello fanout _ from hmdp";
        //发送
        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

}
