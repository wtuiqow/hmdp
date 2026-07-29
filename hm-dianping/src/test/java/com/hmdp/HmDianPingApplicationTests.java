package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.CacheConfig;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private RedissonClient redissonClient;

    @Test
    void testRedisson() throws Exception {
        //获取锁(可重入)，指定锁的名称
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        //判断获取锁成功
        if (isLock) {
            try {
                System.out.println("执行业务");
            } finally {
                //释放锁
                lock.unlock();
            }

        }
    }


}