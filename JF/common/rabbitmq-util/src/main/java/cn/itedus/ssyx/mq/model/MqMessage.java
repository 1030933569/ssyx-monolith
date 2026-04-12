package cn.itedus.ssyx.mq.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class MqMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String exchange;
    private String routingKey;
    private Object payload;
    private long createdAt;
    private long availableAt;
}

