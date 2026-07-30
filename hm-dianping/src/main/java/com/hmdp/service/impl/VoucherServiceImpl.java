package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional      //Spring 事务注解
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
          //MyBatis-Plus 默认规则：**实体类名首字母小写，驼峰转下划线 = 表名**   类名 `Voucher` → 默认表名：**voucher**
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //interface IService<T> 中 定义default boolean save(T entity)
        //此类 extends ServiceImpl<VoucherMapper, Voucher>,
        //而ServiceImpl<M extends BaseMapper<T>, T> implements IService<T>
        //所以 this.save 只能传 Voucher 而不能传 SeckillVoucher

        //秒杀券写入redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY+voucher.getId(),voucher.getStock().toString());
    }

    /*
    1. 传入 `Voucher` 对象（包含基础优惠券信息、库存、开始 / 结束时间）
    2. **save(voucher)**：先向 `voucher` 表插入普通优惠券记录

    > ⚠️ 关键点：MyBatis-Plus 主键自增策略生效后，`voucher.getId()` 才会被回填主键！
    >  在 `save()` **执行之前**，`voucher.getId()` 是 `null`
    >  执行 `save(voucher)` 往数据库插入数据之后，**数据库自动生成主键 ID，MyBatis-Plus 会把这个 ID 重新塞回你传入的 voucher 对象中**；

    3. 构建 `SeckillVoucher` 对象，关联优惠券 ID、库存、秒杀起止时间
    4. `seckillVoucherService.save()`：向 `seckill_voucher` 插入秒杀专属信息
    */

}
