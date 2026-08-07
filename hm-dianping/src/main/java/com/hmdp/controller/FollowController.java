package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    @PutMapping("/{followedUserId}/{isFollow}")
    public Result follow(@PathVariable Long followedUserId, @PathVariable boolean isFollow) {
        return followService.follow(followedUserId,isFollow);
    }

    @GetMapping("/or/not/{followedUserId}")
    public Result notFollow(@PathVariable Long followedUserId) {
        return followService.followOrNot(followedUserId);
    }

}
