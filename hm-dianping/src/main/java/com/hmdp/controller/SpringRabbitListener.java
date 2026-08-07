package com.hmdp.controller;

import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
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

}
