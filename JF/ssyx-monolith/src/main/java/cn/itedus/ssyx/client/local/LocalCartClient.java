package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.cart.service.CartInfoService;
import cn.itedus.ssyx.client.cart.CartFeignClient;
import cn.itedus.ssyx.model.order.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalCartClient implements CartFeignClient {
    @Autowired
    private CartInfoService cartInfoService;

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        return cartInfoService.getCartCheckedList(userId);
    }
}

