package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followedUserId, boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        //true，关注
        if(isFollow == true){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followedUserId);
            this.save(follow);
        }else{
            //delete from tb_follow where user_id = ? and follow_user_id = ?
            remove(new QueryWrapper<Follow>()
            .eq("user_id", userId).eq("follow_user_id",followedUserId));
        }

        return Result.ok();
    }

    @Override
    public Result followOrNot(Long followedUserId) {
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();

        int count = query().eq("user_id", userId).eq("follow_user_id", followedUserId).count();
        return Result.ok(count > 0);
    }
}
