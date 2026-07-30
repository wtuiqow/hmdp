package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
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

    IVoucherOrderService proxy;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId){
        //获取lua脚本需要的参数
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");

        //执行lua，判断购买资格
        Long r = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
                //String.valueOf兼容Long跟long，可以全写资格
        );

        if(r != 0){
            return Result.fail(r == 2 ? "一人一单" : "卖完了");
        }

        //有购买资格,存入阻塞队列，让其他服务完成对mysql的订单写入
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.订单id（新创建的
        voucherOrder.setId(orderId);
        // 2.4.用户id
        voucherOrder.setUserId(userId);
        // 2.5.代金券id（原本有的,指哪种优惠券
        voucherOrder.setVoucherId(voucherId);

        //获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();

        // 2.6.放入阻塞队列
        orderTasks.add(voucherOrder);

        return Result.ok(orderId);
    }

    //异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);

    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true) {
                try {
                    //获取队列订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("VoucherOrderHandler 阻塞队列处理订单异常", e);
                }
            }
        }

        private void handleVoucherOrder(VoucherOrder voucherOrder) {

            //保证创建订单的事务性
            proxy.createVoucherOrder(voucherOrder);
              //异步线程无法拿到 IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
              //需要提前获取，放到父类中

        }
    }



    /*@Override
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

        *//*synchronized (userId.toString().intern()) {
            //toString()每次返回一个值相等的新对象，需要intern()规范字符串返回（如果池中存在匹配 equals() 的对象，返回它
            //锁加在这里会在事务提交完(数据库更改完毕)才释放，加在事务内部可能导致事务未提交，而其他线程已经获取到锁了
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
            //不能（this.）createVoucherOrder(voucherId)，Spring事务生效需要对类进行动态代理，拿代理对象进行事务处理
        }*//*

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

    }*/

    @Transactional      //事务  扣减库存 和 创建订单，必须同时成功、同时失败
    public Result createVoucherOrder(VoucherOrder voucherOrder) {
        //get userid
//        Long userId = UserHolder.getUser().getId();
        long useId = voucherOrder.getUserId();
        /*//userorder
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder).count();
        //if userorder exists ?
        //yes fail
        if (count > 0) {
            return Result.fail("只能购买一张");
        }
        //no continue*/

        //存入阻塞队列前，已经判断过是否有购买资格

        //5，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1").eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("卖完了");
        }

//        //6.创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 6.1.订单id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        /*// 6.2.用户id
//        Long userId = UserHolder.getUser().getId();
//          //`UserHolder` 是一个**基于 ThreadLocal 封装的工具类**，用来在同一**一次请求链路**里，随时随地获取当前登录用户信息*/
//        voucherOrder.setUserId(userId);
//        // 6.3.代金券id
//        voucherOrder.setVoucherId(voucherId);
        //订单被放进阻塞队列中，此处无需再创建，只需把传入参数从id换为完整订单

        save(voucherOrder);

        return Result.ok(voucherOrder.getId());

    }




}
