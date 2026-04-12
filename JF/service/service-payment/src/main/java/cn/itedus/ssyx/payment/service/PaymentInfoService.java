package cn.itedus.ssyx.payment.service;

import cn.itedus.ssyx.enums.PaymentType;
import cn.itedus.ssyx.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PaymentInfoService extends IService<PaymentInfo> {
    PaymentInfo getPaymentInfo(String orderNo, PaymentType paymentType);

    PaymentInfo savePaymentInfo(String orderNo, PaymentType paymentType);

    void mockPaySuccess(String orderNo, PaymentType paymentType);

    void paySuccess(String outTradeNo, PaymentType paymentType, Map<String, String> resultMap);
}
