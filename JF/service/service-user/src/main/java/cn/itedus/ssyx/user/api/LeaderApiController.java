package cn.itedus.ssyx.user.api;

import cn.itedus.ssyx.common.auth.AuthContextHolder;
import cn.itedus.ssyx.common.constant.RedisConst;
import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.model.user.Leader;
import cn.itedus.ssyx.user.service.UserService;
import cn.itedus.ssyx.vo.user.LeaderAddressVo;
import cn.itedus.ssyx.vo.user.LeaderOpenGroupReqVo;
import cn.itedus.ssyx.vo.user.UserLoginVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-19 19:00
 * @description: 团长接口
 */
@Api(tags = "团长接口")
@RestController
@RequestMapping("/api/user/leader")
public class LeaderApiController {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("获取提货点地址信息")
    @GetMapping("/inner/getUserAddressByUserId/{userId}")
    public LeaderAddressVo getUserAddressByUserId(@PathVariable("userId") Long userId) {
        LeaderAddressVo leaderAddressVo = userService.getLeadAddressVoByUserId(userId);
        return leaderAddressVo;
    }

    @ApiOperation("获取当前用户提货点信息")
    @GetMapping("/auth/address")
    public Result getCurrentUserAddress() {
        Long userId = AuthContextHolder.getUserId();
        return Result.ok(userService.getLeadAddressVoByUserId(userId));
    }

    @ApiOperation("用户开团（最简化：只填写提货点信息）")
    @PostMapping("/auth/open")
    public Result openLeader(@RequestBody LeaderOpenGroupReqVo reqVo) {
        LeaderAddressVo leaderAddressVo = userService.openLeader(reqVo);
        this.refreshUserLoginCache();
        return Result.ok(leaderAddressVo);
    }

    @ApiOperation("用户开团申请")
    @PostMapping("/auth/apply")
    public Result applyForLeader(@RequestBody Leader leader) {
        leader.setApplyStatus(1); // 自动通过，设置为已通过
        leader.setApplyTime(new Date());
        leader.setCheckStatus(1); // 同时设置审核状态为通过
        leader.setCheckTime(new Date());
        leader.setCheckUser("system");

        Long leaderId = userService.applyForLeader(leader);

        // 获取最新的提货点信息
        LeaderAddressVo leaderAddressVo = userService.getLeadAddressVoByUserId(AuthContextHolder.getUserId());
        this.refreshUserLoginCache();
        return Result.ok(leaderAddressVo);
    }

    @ApiOperation("用户加入团/选择提货点")
    @GetMapping("/auth/selectLeader/{leaderId}")
    public Result selectLeader(@PathVariable("leaderId") Long leaderId) {
        LeaderAddressVo leaderAddressVo = userService.joinLeader(leaderId);
        this.refreshUserLoginCache();
        return Result.ok(leaderAddressVo);
    }

    private void refreshUserLoginCache() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            return;
        }
        UserLoginVo userLoginVo = userService.getUserLoginVoByUserId(userId);
        redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX + userId, userLoginVo,
                RedisConst.USERKEY_TIMEOUT, TimeUnit.DAYS);
    }
}
