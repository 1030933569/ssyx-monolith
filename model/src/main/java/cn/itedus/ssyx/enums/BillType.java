package cn.itedus.ssyx.enums;

import com.alibaba.fastjson.annotation.JSONType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@JSONType(serializeEnumAsJavaBean = true)
@Getter
public enum BillType {
    ORDER(0,"订单佣金"),
    WITHDRAW(1,"提现" ),
    REFUND(1,"订单退款" );

    @EnumValue
    @JsonValue
    private Integer code ;
    private String comment ;

    BillType(Integer code, String comment ){
        this.code=code;
        this.comment=comment;
    }
}