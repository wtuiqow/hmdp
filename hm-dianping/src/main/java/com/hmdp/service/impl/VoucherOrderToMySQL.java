package com.hmdp.service.impl;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class VoucherOrderToMySQL {

    @Autowired
    IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "hmdp.queue1")
    public void listenFanoutQueue_1(VoucherOrder voucherOrder) throws InterruptedException {

        //订单创建，跨bean调用调用的是代理对象，能够保证事务一致性
        voucherOrderService.createVoucherOrder(voucherOrder);

        System.out.println("消费者 接收到 hmdp.queue1 的消息：" + voucherOrder + LocalTime.now());
    }

}
