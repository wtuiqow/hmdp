package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
    @TableId.作用：**标记实体类主键字段**
    常见用法
    // 主键字段名（数据库列名）
    @TableId(value = "user\_id")
    // 主键生成策略
    @TableId(type = IdType.AUTO)
    private Long userId;
    */

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商铺id
     */
    private Long shopId;

    /**
     * 代金券标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则
     */
    private String rules;

    /**
     * 支付金额
     */
    private Long payValue;

    /**
     * 抵扣金额
     */
    private Long actualValue;

    /**
     * 优惠券类型
     */
    private Integer type;

    /**
     * 优惠券类型
     */
    private Integer status;

    /*
    @TableField.作用：标记**普通数据库字段**
    常见用法
    // 指定数据库列名（驼峰与下划线不一致时使用）
    @TableField("user\_name")
    private String userName;

    // 查询时不返回该字段（大字段、密码）
    @TableField(select = false)
    private String password;

    // 字段不存在数据库，只是实体临时属性
    @TableField(exist = false)
    private String tempInfo;
    */

    /**
     * 库存
     */
    @TableField(exist = false)      //对象里有但数据表里没有的属性
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
