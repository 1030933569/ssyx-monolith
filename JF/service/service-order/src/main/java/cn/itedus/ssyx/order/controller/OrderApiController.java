package cn.itedus.ssyx.order.controller;

import cn.itedus.ssyx.common.auth.AuthContextHolder;
import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.model.order.OrderInfo;
import cn.itedus.ssyx.order.service.OrderInfoService;
import cn.itedus.ssyx.vo.order.OrderConfirmVo;
import cn.itedus.ssyx.vo.order.OrderSubmitVo;
import cn.itedus.ssyx.vo.order.OrderUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Order Management API")
@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    @ApiOperation("Confirm order")
    @GetMapping("auth/confirmOrder")
    public Result confirmOrder() {
        OrderConfirmVo orderConfirmVo = orderInfoService.confirmOrder();
        return Result.ok(orderConfirmVo);
    }

    @ApiOperation("Submit order")
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderSubmitVo orderSubmitVo) {
        Long orderId = orderInfoService.submitOrder(orderSubmitVo);
        return Result.ok(orderId);
    }

    @ApiOperation("Get order detail")
    @GetMapping("auth/getOrderInfoById/{orderId}")
    public Result getOrderInfoById(@PathVariable("orderId") Long orderId) {
        OrderInfo orderInfo = orderInfoService.getOrderInfoById(orderId);
        return Result.ok(orderInfo);
    }

    @ApiOperation("Get order by order number")
    @GetMapping("inner/getOrderInfoByOrderNo/{orderNo}")
    public OrderInfo getOrderInfoByOrderNo(@PathVariable("orderNo") String orderNo) {
        return orderInfoService.getOrderInfoByOrderNo(orderNo);
    }

    @ApiOperation("Advance order payment status")
    @GetMapping("inner/orderPay/{orderNo}")
    public void orderPay(@PathVariable("orderNo") String orderNo) {
        orderInfoService.orderPay(orderNo);
    }

    @ApiOperation("List user orders by page")
    @GetMapping("auth/findUserOrderPage/{page}/{limit}")
    public Result findUserOrderPage(@PathVariable("page") Long page,
                                    @PathVariable("limit") Long limit,
                                    OrderUserQueryVo orderUserQueryVo) {
        Long userId = AuthContextHolder.getUserId();
        orderUserQueryVo.setUserId(userId);
        Page<OrderInfo> pageModel = new Page<>(page, limit);
        IPage<OrderInfo> iPage = orderInfoService.findUserOrderPage(pageModel, orderUserQueryVo);
        return Result.ok(iPage);
    }
}
