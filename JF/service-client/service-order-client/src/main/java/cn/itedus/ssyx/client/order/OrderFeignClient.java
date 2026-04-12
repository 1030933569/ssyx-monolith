package cn.itedus.ssyx.client.order;

import cn.itedus.ssyx.model.order.OrderInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface OrderFeignClient {
    /**
     * Get the order by order number.
     *
     * @param orderNo order number
     * @return order info
     */
    @GetMapping("/api/order/inner/getOrderInfoByOrderNo/{orderNo}")
    OrderInfo getOrderInfoByOrderNo(@PathVariable("orderNo") String orderNo);

    /**
     * Advance the order payment status.
     *
     * @param orderNo order number
     */
    @GetMapping("/api/order/inner/orderPay/{orderNo}")
    void orderPay(@PathVariable("orderNo") String orderNo);
}
