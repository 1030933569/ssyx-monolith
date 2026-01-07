package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.activity.client.ActivityFeignClient;
import cn.itedus.ssyx.activity.service.ActivityInfoService;
import cn.itedus.ssyx.activity.service.CouponInfoService;
import cn.itedus.ssyx.model.activity.CouponInfo;
import cn.itedus.ssyx.model.order.CartInfo;
import cn.itedus.ssyx.vo.order.CartInfoVo;
import cn.itedus.ssyx.vo.order.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocalActivityClient implements ActivityFeignClient {
    @Autowired
    private ActivityInfoService activityInfoService;

    @Autowired
    private CouponInfoService couponInfoService;

    @Override
    public Map<Long, List<String>> findActivity(List<Long> skuIdList) {
        return activityInfoService.findActivity(skuIdList);
    }

    @Override
    public Map<String, Object> findActivityAndCoupon(Long skuId, Long userId) {
        return activityInfoService.findActivityAndCoupon(skuId, userId);
    }

    @Override
    public OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId) {
        return activityInfoService.findCartActivityAndCoupon(cartInfoList, userId);
    }

    @Override
    public List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList) {
        return activityInfoService.findCartActivityList(cartInfoList);
    }

    @Override
    public CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId) {
        return couponInfoService.findRangeSkuIdList(cartInfoList, couponId);
    }

    @Override
    public Boolean updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId) {
        couponInfoService.updateCouponInfoUseStatus(couponId, userId, orderId);
        return true;
    }
}

