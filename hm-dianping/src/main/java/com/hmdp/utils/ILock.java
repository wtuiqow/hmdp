package com.hmdp.utils;

import java.util.concurrent.TimeUnit;

public interface ILock {

    //获取锁及释放锁
    boolean tryLock(long timeoutSec);

    boolean unLock();
}
