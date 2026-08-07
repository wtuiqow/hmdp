package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;

import com.hmdp.utils.UserHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    /*Blog 实体上标了 @TableName("tb_blog")，而 blogService 对应的
    BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>，
    MyBatis-Plus 会从泛型 Blog 推断出要操作的表，也就是 tb_blog*/

    @Resource
    private IUserService userService;

    @Override
    public Result queryBlogById(Long id) {
        //1.查blog
        Blog blog = getById(id);
        //2.查blog相关用户信息
        queryBlogUser(blog);
        //3.查询是否已经被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });

        return Result.ok(records);
    }

    private void isBlogLiked(Blog blog) {
        //获取当前用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        blog.setIsLike(BooleanUtil.isTrue(isMember));
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 第56行：声明一个静态的脚本对象
    private static final DefaultRedisScript<Long> LIKED_SCRIPT;
    static{
        LIKED_SCRIPT = new DefaultRedisScript<>();
        LIKED_SCRIPT.setLocation(new ClassPathResource("likeBlog.lua"));
        LIKED_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendDirectExchange(Long blogId) throws InterruptedException {
        //交换机名
        String exchangeName = "hmdp.direct";
        //消息，此处无用
        String message = "点赞更新完成";
        //发送
        rabbitTemplate.convertAndSend(exchangeName, "likeBlog", blogId);
    }

    @Override
    public Result likeBlog(Long id) throws InterruptedException {
        //获取当前用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();

        // Redis 缓存丢了，先用 MySQL 的 liked 初始化，再执行 Lua
        String countKey = "blog:liked:count:" + id;
        if (stringRedisTemplate.opsForValue().get(countKey) == null) {
            Blog blog = getById(id);
            if (blog != null) {
                stringRedisTemplate.opsForValue().set(countKey, String.valueOf(blog.getLiked()));
            }
        }

        //执行lua，判断是否点赞，并在redis数据库执行对应操作（点赞或取消点赞并更新点赞状态
        Long r = stringRedisTemplate.execute(
                LIKED_SCRIPT,
                Arrays.asList(BLOG_LIKED_KEY + id, "blog:liked:count:" + id),
                String.valueOf(userId)
        );

        sendDirectExchange(id);

        return Result.ok();
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
