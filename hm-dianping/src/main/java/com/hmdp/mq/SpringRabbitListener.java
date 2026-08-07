package com.hmdp.mq;

import com.hmdp.entity.Blog;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SpringRabbitListener {

    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IBlogService blogService;

    public SpringRabbitListener(@Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.likeBlog.queue1"),
            exchange = @Exchange(name = "hmdp.direct",type = "direct"),
            key = {"likeBlog"}
    ))
    public void listenDirectQueue_1(Long blogId) throws InterruptedException {

        //更新redis数据到mysql
        String key = "blog:liked:count:" + blogId;
        Object o = stringRedisTemplate.opsForValue().get(key);
        Long count = o == null ? 0L : Long.valueOf(o.toString());

        blogService.lambdaUpdate()
                .eq(Blog::getId, blogId)
                .set(Blog::getLiked, Long.valueOf(count))
                .update();

        System.out.println("接收到 direct.likeBlog.queue1 的消息：" + blogId + LocalTime.now());
    }

    //秒杀订单
    @Autowired
    IVoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.likeBlog.queue2"),
            exchange = @Exchange(name = "hmdp.direct",type = "direct"),
            key = {"secKill"}
    ))
    public void listenDirectQueue_2(VoucherOrder voucherOrder){

        //订单创建，跨bean调用调用的是代理对象，能够保证事务一致性
        voucherOrderService.createVoucherOrder(voucherOrder);

        System.out.println("消费者 接收到 hmdp.queue1 的消息：" + voucherOrder + LocalTime.now());
    }

}
