package cn.itedus.ssyx.enums;

import com.alibaba.fastjson.annotation.JSONType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@JSONType(serializeEnumAsJavaBean = true)
@Getter
public enum UserType {
    USER(0,"会员"),
    LEADER(1,"团长" );

    @EnumValue
    @JsonValue
    private Integer code ;
    private String comment ;

    UserType(Integer code, String comment ){
        this.code=code;
        this.comment=comment;
    }
}