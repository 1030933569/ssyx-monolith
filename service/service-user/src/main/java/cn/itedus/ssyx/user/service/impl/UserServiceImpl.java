package cn.itedus.ssyx.user.service.impl;

import cn.itedus.ssyx.common.auth.AuthContextHolder;
import cn.itedus.ssyx.common.exception.SsyxException;
import cn.itedus.ssyx.common.result.ResultCodeEnum;
import cn.itedus.ssyx.enums.UserType;
import cn.itedus.ssyx.model.user.Leader;
import cn.itedus.ssyx.model.user.User;
import cn.itedus.ssyx.model.user.UserDelivery;
import cn.itedus.ssyx.user.mapper.LeaderMapper;
import cn.itedus.ssyx.user.mapper.UserDeliveryMapper;
import cn.itedus.ssyx.user.mapper.UserMapper;
import cn.itedus.ssyx.user.service.UserService;
import cn.itedus.ssyx.vo.user.LeaderAddressVo;
import cn.itedus.ssyx.vo.user.LeaderOpenGroupReqVo;
import cn.itedus.ssyx.vo.user.UserLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-16 17:02
 * @description: 微信端用户服务接口实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDeliveryMapper userDeliveryMapper;
    @Autowired
    private LeaderMapper leaderMapper;

    private Long getRequiredLoginUserId() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            throw new SsyxException(ResultCodeEnum.LOGIN_AUTH);
        }
        return userId;
    }

    private void clearDefaultDelivery(Long userId) {
        UserDelivery updateEntity = new UserDelivery();
        updateEntity.setIsDefault(0);
        userDeliveryMapper.update(updateEntity,
                new LambdaQueryWrapper<UserDelivery>()
                        .eq(UserDelivery::getUserId, userId)
                        .eq(UserDelivery::getIsDeleted, 0)
                        .eq(UserDelivery::getIsDefault, 1));
    }

    @Override
    public User getByOpenId(String openId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        return user;
    }

    @Override
    public LeaderAddressVo getLeadAddressVoByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<UserDelivery> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserDelivery::getUserId, userId);
        lambdaQueryWrapper.eq(UserDelivery::getIsDeleted, 0);
        lambdaQueryWrapper.eq(UserDelivery::getIsDefault, 1);
        lambdaQueryWrapper.orderByDesc(UserDelivery::getUpdateTime);
        lambdaQueryWrapper.last("LIMIT 1");
        UserDelivery userDelivery = userDeliveryMapper.selectOne(lambdaQueryWrapper);

        Leader leader = resolveValidLeader(userDelivery);
        if (leader == null) {
            // 兜底：用户没有提货点时自动绑定一个可用提货点，保证业务流程可跑通（毕设场景）
            userDelivery = ensureDefaultUserDelivery(userId);
            leader = resolveValidLeader(userDelivery);
        }
        if (leader == null || userDelivery == null) {
            return null;
        }
        LeaderAddressVo leaderAddressVo = new LeaderAddressVo();
        BeanUtils.copyProperties(leader, leaderAddressVo);
        leaderAddressVo.setUserId(userId);
        leaderAddressVo.setLeaderId(leader.getId());
        leaderAddressVo.setLeaderName(leader.getName());
        leaderAddressVo.setLeaderPhone(leader.getPhone());
        leaderAddressVo.setWareId(userDelivery.getWareId() != null ? userDelivery.getWareId() : 1L);
        leaderAddressVo.setStorePath(leader.getStorePath());
        return leaderAddressVo;
    }

    @Override
    public UserLoginVo getUserLoginVoByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        UserLoginVo userLoginVo = new UserLoginVo();
        if (user != null) {
            userLoginVo.setPhotoUrl(user.getPhotoUrl());
            userLoginVo.setNickName(user.getNickName());
            userLoginVo.setOpenId(user.getOpenId());
            userLoginVo.setIsNew(user.getIsNew());
        }
        userLoginVo.setUserId(userId);

        UserDelivery userDelivery = userDeliveryMapper.selectOne(
                new LambdaQueryWrapper<UserDelivery>().eq(UserDelivery::getUserId, userId)
                        .eq(UserDelivery::getIsDeleted, 0)
                        .eq(UserDelivery::getIsDefault, 1)
                        .orderByDesc(UserDelivery::getUpdateTime)
                        .last("LIMIT 1")
        );

        // 缓存里必须有 leaderId/wareId，避免前端/下单流程卡在“请选择提货点”
        if (resolveValidLeader(userDelivery) == null) {
            userDelivery = ensureDefaultUserDelivery(userId);
        }
        if (userDelivery != null && userDelivery.getLeaderId() != null) {
            userLoginVo.setLeaderId(userDelivery.getLeaderId());
            userLoginVo.setWareId(userDelivery.getWareId() != null ? userDelivery.getWareId() : 1L);
        } else {
            userLoginVo.setLeaderId(1L);
            userLoginVo.setWareId(1L);
        }
        return userLoginVo;

    }

    /**
     * 兜底：保证当前用户一定有一个默认提货点绑定（user_delivery.is_default=1）。
     * <p>
     * 触发条件：用户未绑定默认提货点、或绑定的提货点不存在/已删除/未审核通过。
     */
    private UserDelivery ensureDefaultUserDelivery(Long userId) {
        if (userId == null) {
            return null;
        }

        // 双重检查：避免每次都写库
        UserDelivery current = getLatestDefaultDelivery(userId);
        if (resolveValidLeader(current) != null) {
            return current;
        }

        Leader leader = findAnyAvailableLeader();
        if (leader == null) {
            leader = createFallbackLeader(userId);
        }
        if (leader == null || leader.getId() == null) {
            return null;
        }

        return bindDefaultDelivery(userId, leader.getId(), 1L);
    }

    private UserDelivery getLatestDefaultDelivery(Long userId) {
        return userDeliveryMapper.selectOne(
                new LambdaQueryWrapper<UserDelivery>()
                        .eq(UserDelivery::getUserId, userId)
                        .eq(UserDelivery::getIsDeleted, 0)
                        .eq(UserDelivery::getIsDefault, 1)
                        .orderByDesc(UserDelivery::getUpdateTime)
                        .last("LIMIT 1")
        );
    }

    private Leader resolveValidLeader(UserDelivery userDelivery) {
        if (userDelivery == null || userDelivery.getLeaderId() == null) {
            return null;
        }
        Leader leader = leaderMapper.selectById(userDelivery.getLeaderId());
        if (leader == null) {
            return null;
        }
        if (Objects.equals(leader.getIsDeleted(), 1)) {
            return null;
        }
        // 提货点列表接口会过滤 check_status=1，这里也保持一致，避免绑定到不可用提货点
        if (!Objects.equals(leader.getCheckStatus(), 1)) {
            return null;
        }
        return leader;
    }

    private Leader findAnyAvailableLeader() {
        return leaderMapper.selectOne(
                new LambdaQueryWrapper<Leader>()
                        .eq(Leader::getIsDeleted, 0)
                        .eq(Leader::getCheckStatus, 1)
                        .orderByAsc(Leader::getId)
                        .last("LIMIT 1")
        );
    }

    private Leader createFallbackLeader(Long userId) {
        User user = userMapper.selectById(userId);

        Leader leader = new Leader();
        leader.setUserId(userId);

        String nickName = user != null ? user.getNickName() : null;
        leader.setName(StringUtils.hasText(nickName) ? nickName : ("系统团长" + userId));

        String phone = user != null ? user.getPhone() : null;
        leader.setPhone(StringUtils.hasText(phone) ? phone : "0");

        leader.setTakeName("默认提货点");
        leader.setDetailAddress("默认提货点地址");
        leader.setLongitude(0D);
        leader.setLatitude(0D);
        leader.setHaveStore(1);
        leader.setStorePath("");
        leader.setWorkStatus(0);
        leader.setTakeType("1");
        leader.setCheckStatus(1);
        leader.setCheckTime(new Date());
        leader.setCheckUser("system");
        leader.setIsDeleted(0);

        // 不设置 applyStatus/applyTime，兼容未执行 add_leader_apply.sql 的库结构
        leaderMapper.insert(leader);
        return leader;
    }

    private UserDelivery bindDefaultDelivery(Long userId, Long leaderId, Long wareId) {
        if (userId == null || leaderId == null) {
            return null;
        }
        Long safeWareId = wareId != null ? wareId : 1L;

        this.clearDefaultDelivery(userId);

        UserDelivery existing = userDeliveryMapper.selectOne(
                new LambdaQueryWrapper<UserDelivery>()
                        .eq(UserDelivery::getUserId, userId)
                        .eq(UserDelivery::getLeaderId, leaderId)
                        .eq(UserDelivery::getIsDeleted, 0)
                        .orderByDesc(UserDelivery::getUpdateTime)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            UserDelivery update = new UserDelivery();
            update.setId(existing.getId());
            update.setIsDefault(1);
            if (existing.getWareId() == null) {
                update.setWareId(safeWareId);
                existing.setWareId(safeWareId);
            }
            userDeliveryMapper.updateById(update);
            existing.setIsDefault(1);
            return existing;
        }

        UserDelivery insert = new UserDelivery();
        insert.setUserId(userId);
        insert.setLeaderId(leaderId);
        insert.setWareId(safeWareId);
        insert.setIsDefault(1);
        insert.setCreateTime(new Date());
        insert.setUpdateTime(new Date());
        insert.setIsDeleted(0);
        userDeliveryMapper.insert(insert);
        return insert;
    }

    @Override
    @Transactional
    public Long applyForLeader(Leader leader) {
        if (leader == null) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        // 获取当前登录用户ID
        Long userId = this.getRequiredLoginUserId();

        // 兜底必填信息（最简化开团时前端可能不传 name/phone）
        User user = userMapper.selectById(userId);
        if (!StringUtils.hasText(leader.getName())) {
            String nickName = user != null ? user.getNickName() : null;
            leader.setName(StringUtils.hasText(nickName) ? nickName : ("团长" + userId));
        }
        if (!StringUtils.hasText(leader.getPhone())) {
            String phone = user != null ? user.getPhone() : null;
            leader.setPhone(StringUtils.hasText(phone) ? phone : "0");
        }

        // 保存团长信息
        leader.setUserId(userId);
        leader.setCheckStatus(1); // 自动审核通过
        leader.setCheckTime(new Date());
        leader.setCheckUser("system");
        leader.setApplyStatus(1); // 开团已通过
        leader.setApplyTime(new Date());
        leader.setWorkStatus(0); // 营业状态：营业中
        leader.setHaveStore(1); // 有门店
        leader.setTakeType("1"); // 默认类型：宝妈
        leader.setIsDeleted(0);

        leaderMapper.insert(leader);

        // 保存用户与团长的关联关系（默认提货点）
        this.clearDefaultDelivery(userId);
        UserDelivery userDelivery = new UserDelivery();
        userDelivery.setUserId(userId);
        userDelivery.setLeaderId(leader.getId());
        userDelivery.setWareId(1L);
        userDelivery.setIsDefault(1);
        userDelivery.setCreateTime(new Date());
        userDelivery.setUpdateTime(new Date());
        userDelivery.setIsDeleted(0);

        userDeliveryMapper.insert(userDelivery);

        return leader.getId();
    }

    @Override
    @Transactional
    public LeaderAddressVo openLeader(LeaderOpenGroupReqVo reqVo) {
        if (reqVo == null || !StringUtils.hasText(reqVo.getTakeName()) || !StringUtils.hasText(reqVo.getDetailAddress())) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }

        Leader leader = new Leader();
        leader.setTakeName(reqVo.getTakeName());
        leader.setDetailAddress(reqVo.getDetailAddress());
        leader.setLongitude(reqVo.getLongitude());
        leader.setLatitude(reqVo.getLatitude());
        leader.setStorePath(reqVo.getStorePath());

        this.applyForLeader(leader);

        Long userId = this.getRequiredLoginUserId();
        return this.getLeadAddressVoByUserId(userId);
    }

    @Override
    @Transactional
    public LeaderAddressVo joinLeader(Long leaderId) {
        Long userId = this.getRequiredLoginUserId();
        if (leaderId == null) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }

        Leader leader = leaderMapper.selectById(leaderId);
        if (leader == null || (leader.getIsDeleted() != null && leader.getIsDeleted() == 1) || (leader.getCheckStatus() != null && leader.getCheckStatus() != 1)) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }

        this.clearDefaultDelivery(userId);

        LambdaQueryWrapper<UserDelivery> query = new LambdaQueryWrapper<UserDelivery>()
                .eq(UserDelivery::getUserId, userId)
                .eq(UserDelivery::getLeaderId, leaderId)
                .eq(UserDelivery::getIsDeleted, 0)
                .orderByDesc(UserDelivery::getUpdateTime)
                .last("LIMIT 1");
        UserDelivery existing = userDeliveryMapper.selectOne(query);
        if (existing != null) {
            UserDelivery update = new UserDelivery();
            update.setId(existing.getId());
            update.setIsDefault(1);
            if (existing.getWareId() == null) {
                update.setWareId(1L);
            }
            userDeliveryMapper.updateById(update);
        } else {
            UserDelivery insert = new UserDelivery();
            insert.setUserId(userId);
            insert.setLeaderId(leaderId);
            insert.setWareId(1L);
            insert.setIsDefault(1);
            insert.setCreateTime(new Date());
            insert.setUpdateTime(new Date());
            insert.setIsDeleted(0);
            userDeliveryMapper.insert(insert);
        }

        return this.getLeadAddressVoByUserId(userId);
    }
}
