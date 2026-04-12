package cn.itedus.ssyx.client.order;

import cn.itedus.ssyx.model.order.OrderInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author: Guanghao Wei
 * @date: 2023-07-06 15:04
 * @description:
 */
public interface OrderFeignClient {
    /**
     * 根据订单编号获取订单项
     *
     * @param orderNo 订单编号
     * @return 订单项
     */
    @GetMapping("/api/order/inner/getOrderInfoByOrderNo/{orderNo}")
    public OrderInfo getOrderInfoByOrderNo(@PathVariable("orderNo") String orderNo);

    /**
     * 按订单号推进订单支付状态
     *
     * @param orderNo 订单号
     */
    @GetMapping("/api/order/inner/orderPay/{orderNo}")
    public void orderPay(@PathVariable("orderNo") String orderNo);
}
