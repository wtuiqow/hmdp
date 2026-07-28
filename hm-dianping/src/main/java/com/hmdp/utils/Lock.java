package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class Lock implements ILock{

    String name;
    StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";

    public Lock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {

        long threadId = Thread.currentThread().getId();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lock);
    }

    @Override
    public boolean unLock() {
        // 获取线程标示
        String threadId = KEY_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标示
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if(threadId.equals(id)) {
            Boolean lock = stringRedisTemplate.delete(KEY_PREFIX + name);
            return Boolean.TRUE.equals(lock);
        }
        return false;
    }
}
