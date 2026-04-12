package cn.itedus.ssyx.payment.service.impl;

import cn.itedus.ssyx.client.order.OrderFeignClient;
import cn.itedus.ssyx.common.exception.SsyxException;
import cn.itedus.ssyx.common.result.ResultCodeEnum;
import cn.itedus.ssyx.enums.PaymentStatus;
import cn.itedus.ssyx.enums.PaymentType;
import cn.itedus.ssyx.model.order.OrderInfo;
import cn.itedus.ssyx.model.order.PaymentInfo;
import cn.itedus.ssyx.mq.constant.MqConst;
import cn.itedus.ssyx.mq.service.RabbitService;
import cn.itedus.ssyx.payment.mapper.PaymentInfoMapper;
import cn.itedus.ssyx.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public PaymentInfo getPaymentInfo(String orderNo, PaymentType paymentType) {
        return paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo)
                .eq(PaymentInfo::getPaymentType, paymentType));
    }

    @Override
    public PaymentInfo savePaymentInfo(String orderNo, PaymentType paymentType) {
        OrderInfo order = orderFeignClient.getOrderInfoByOrderNo(orderNo);
        if (order == null) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setUserId(order.getUserId());
        paymentInfo.setOrderNo(order.getOrderNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject("test");
        paymentInfo.setTotalAmount(new BigDecimal("0.01"));

        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;
    }

    @Override
    public void mockPaySuccess(String orderNo, PaymentType paymentType) {
        PaymentInfo paymentInfo = this.getPaymentInfo(orderNo, paymentType);
        if (paymentInfo == null) {
            paymentInfo = this.savePaymentInfo(orderNo, paymentType);
        }

        if (paymentInfo.getPaymentStatus() != PaymentStatus.PAID) {
            Map<String, String> resultMap = new HashMap<>();
            String tradeNo = "mock_" + System.currentTimeMillis();
            resultMap.put("out_trade_no", orderNo);
            resultMap.put("trade_state", "SUCCESS");
            resultMap.put("transaction_id", tradeNo);
            resultMap.put("trade_no", tradeNo);
            this.paySuccess(orderNo, paymentType, resultMap);
        }

        // Advance the order immediately so the list page reflects payment without MQ delay.
        orderFeignClient.orderPay(orderNo);
    }

    @Override
    public void paySuccess(String outTradeNo, PaymentType paymentType, Map<String, String> paramMap) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, outTradeNo)
                .eq(PaymentInfo::getPaymentType, paymentType));
        if (paymentInfo == null) {
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        if (paymentInfo.getPaymentStatus() != PaymentStatus.UNPAID) {
            return;
        }

        PaymentInfo paymentInfoUpd = new PaymentInfo();
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
        paymentInfoUpd.setTradeNo(resolveTradeNo(outTradeNo, paymentType, paramMap));
        paymentInfoUpd.setCallbackTime(new Date());
        paymentInfoUpd.setCallbackContent(paramMap.toString());
        paymentInfoMapper.update(paymentInfoUpd, new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, outTradeNo));

        rabbitService.sendMessage(MqConst.EXCHANGE_PAY_DIRECT, MqConst.ROUTING_PAY_SUCCESS, outTradeNo);
    }

    private String resolveTradeNo(String orderNo, PaymentType paymentType, Map<String, String> paramMap) {
        String tradeNo;
        if (paymentType == PaymentType.WEIXIN) {
            tradeNo = paramMap.get("transaction_id");
            if (isBlank(tradeNo)) {
                tradeNo = paramMap.get("ransaction_id");
            }
        } else {
            tradeNo = paramMap.get("trade_no");
        }

        if (isBlank(tradeNo)) {
            tradeNo = paramMap.get("trade_no");
        }
        return isBlank(tradeNo) ? orderNo : tradeNo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
