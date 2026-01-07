package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.client.user.UserFeignClient;
import cn.itedus.ssyx.user.service.UserService;
import cn.itedus.ssyx.vo.user.LeaderAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalUserClient implements UserFeignClient {
    @Autowired
    private UserService userService;

    @Override
    public LeaderAddressVo getUserAddressByUserId(Long userId) {
        return userService.getLeadAddressVoByUserId(userId);
    }
}

