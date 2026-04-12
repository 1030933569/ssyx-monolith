package cn.itedus.ssyx.payment.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.common.result.ResultCodeEnum;
import cn.itedus.ssyx.enums.PaymentType;
import cn.itedus.ssyx.payment.service.PaymentInfoService;
import cn.itedus.ssyx.payment.service.WeixinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "微信支付接口")
@RestController
@RequestMapping("/api/payment/weixin")
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @ApiOperation("下单小程序支付")
    @GetMapping("/createJsapi/{orderNo}")
    public Result createJsapi(@PathVariable("orderNo") String orderNo) {
        Map<String, String> result = weixinService.createJsapi(orderNo);
        return Result.ok(result);
    }

    @ApiOperation("开发用 mock 支付成功")
    @PostMapping("auth/mockPay/{orderNo}")
    public Result mockPay(@PathVariable("orderNo") String orderNo) {
        paymentInfoService.mockPaySuccess(orderNo, PaymentType.WEIXIN);
        return Result.ok();
    }

    @ApiOperation("查询微信支付状态")
    @GetMapping("queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable("orderNo") String orderNo) {
        Map<String, String> resultMap = weixinService.queryPayStatus(orderNo, PaymentType.WEIXIN);
        if (resultMap == null) {
            return Result.build(null, ResultCodeEnum.PAYMENT_ERROR);
        }
        if ("SUCCESS".equals(resultMap.get("trade_state"))) {
            String outTradeNo = resultMap.get("out_trade_no");
            paymentInfoService.paySuccess(outTradeNo, PaymentType.WEIXIN, resultMap);
            return Result.build(null, ResultCodeEnum.PAYMENT_SUCCESS);
        }
        return Result.build(null, ResultCodeEnum.PAYMENT_DOING);
    }
}
