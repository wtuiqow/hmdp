package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ILock;
import com.hmdp.utils.Lock;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if(LocalDateTime.now().isBefore(voucher.getBeginTime())){
            return Result.fail("活动未开始");
        }
        // 3.判断秒杀是否已经结束
        if(LocalDateTime.now().isAfter(voucher.getEndTime())){
            return Result.fail("活动已结束");
        }
        // 4.判断库存是否充足
        if(voucher.getStock() < 1){
            return Result.fail("卖完了");
        }

        Long userId = UserHolder.getUser().getId();

        /*synchronized (userId.toString().intern()) {
            //toString()每次返回一个值相等的新对象，需要intern()规范字符串返回（如果池中存在匹配 equals() 的对象，返回它
            //锁加在这里会在事务提交完(数据库更改完毕)才释放，加在事务内部可能导致事务未提交，而其他线程已经获取到锁了
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
            //不能（this.）createVoucherOrder(voucherId)，Spring事务生效需要对类进行动态代理，拿代理对象进行事务处理
        }*/

        //Lock lock = new Lock("order:"+userId,stringRedisTemplate);

        RLock lock = redissonClient.getLock("lock:order:" + userId);

        if(!lock.tryLock()){
            return Result.fail("一人一单");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally{
            //出现异常时释放锁
            lock.unlock();
        }

    }

    @Transactional      //事务  扣减库存 和 创建订单，必须同时成功、同时失败
    public Result createVoucherOrder(Long voucherId) {
        //get userid
        Long userId = UserHolder.getUser().getId();
        //userorder
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //if userorder exists ?
        //yes fail
        if (count > 0) {
            return Result.fail("只能购买一张");
        }
        //no continue

        //5，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1").eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("卖完了");
        }

        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        /*// 6.2.用户id
        Long userId = UserHolder.getUser().getId();
          //`UserHolder` 是一个**基于 ThreadLocal 封装的工具类**，用来在同一**一次请求链路**里，随时随地获取当前登录用户信息*/
        voucherOrder.setUserId(userId);
        // 6.3.代金券id
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        return Result.ok(orderId);

    }




}
