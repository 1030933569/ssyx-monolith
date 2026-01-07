package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.client.order.OrderFeignClient;
import cn.itedus.ssyx.model.order.OrderInfo;
import cn.itedus.ssyx.order.service.OrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalOrderClient implements OrderFeignClient {
    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        return orderInfoService.getOrderInfoByOrderNo(orderNo);
    }
}

