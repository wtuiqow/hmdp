package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.CacheConfig;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    RedisIdWorker redisIdWorker;

    //线程池建立
    ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        //计数
        CountDownLatch countDownLatch = new CountDownLatch(300);

        //任务建立
        Runnable runnable = () -> {
            for (int i = 0; i < 100; i++) {
                Long id = redisIdWorker.nextId("order");
                System.out.println("id: "+id);
            }
            countDownLatch.countDown();
        };
        long start = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            //提交任务
            es.submit(runnable);
        }
        //等待全部完成
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end-start));

    }

    @Test
    void testSaveShop(){
        shopService.saveShopToRedis(1L,10L);
    }


}