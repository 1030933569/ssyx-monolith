package cn.itedus.ssyx.mq.service;

import cn.itedus.ssyx.mq.constant.MqConst;
import cn.itedus.ssyx.mq.model.MqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 简易消息队列实现：发送方写入 Redis，消费方由单体应用内部线程消费并分发。
 */
@Service
public class RabbitService {
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    public boolean sendMessage(String exchange, String routingKey, Object message) {
        MqMessage mqMessage = new MqMessage();
        mqMessage.setExchange(exchange);
        mqMessage.setRoutingKey(routingKey);
        mqMessage.setPayload(message);
        mqMessage.setCreatedAt(System.currentTimeMillis());
        mqMessage.setAvailableAt(mqMessage.getCreatedAt());
        redisTemplate.opsForList().leftPush(MqConst.MQ_QUEUE_KEY, mqMessage);
        return true;
    }

    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        MqMessage mqMessage = new MqMessage();
        mqMessage.setExchange(exchange);
        mqMessage.setRoutingKey(routingKey);
        mqMessage.setPayload(message);
        mqMessage.setCreatedAt(System.currentTimeMillis());
        mqMessage.setAvailableAt(mqMessage.getCreatedAt() + delayTime * 1000L);
        redisTemplate.opsForZSet().add(MqConst.MQ_DELAY_KEY, mqMessage, mqMessage.getAvailableAt());
        return true;
    }
}

