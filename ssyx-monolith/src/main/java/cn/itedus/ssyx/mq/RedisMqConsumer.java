package cn.itedus.ssyx.mq;

import cn.itedus.ssyx.cart.service.CartInfoService;
import cn.itedus.ssyx.mq.constant.MqConst;
import cn.itedus.ssyx.mq.model.MqMessage;
import cn.itedus.ssyx.order.service.OrderInfoService;
import cn.itedus.ssyx.product.service.SkuInfoService;
import cn.itedus.ssyx.search.service.SkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisMqConsumer implements SmartLifecycle {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final CartInfoService cartInfoService;
    private final SkuInfoService skuInfoService;
    private final OrderInfoService orderInfoService;
    private final SkuService skuService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "redis-mq-consumer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running = false;

    public RedisMqConsumer(
            RedisTemplate<Object, Object> redisTemplate,
            CartInfoService cartInfoService,
            SkuInfoService skuInfoService,
            OrderInfoService orderInfoService,
            SkuService skuService
    ) {
        this.redisTemplate = redisTemplate;
        this.cartInfoService = cartInfoService;
        this.skuInfoService = skuInfoService;
        this.orderInfoService = orderInfoService;
        this.skuService = skuService;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::pollLoop);
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void pollLoop() {
        while (running) {
            try {
                moveDueDelayedMessages();

                Object messageObj = redisTemplate.opsForList().rightPop(MqConst.MQ_QUEUE_KEY, 5, TimeUnit.SECONDS);
                if (messageObj == null) {
                    continue;
                }
                if (!(messageObj instanceof MqMessage)) {
                    log.warn("Skip unknown mq message type: {}", messageObj.getClass().getName());
                    continue;
                }
                dispatch((MqMessage) messageObj);
            } catch (Exception ex) {
                log.error("Redis MQ consume failed", ex);
                sleepQuietly(1000);
            }
        }
    }

    private void moveDueDelayedMessages() {
        long now = System.currentTimeMillis();
        Set<Object> dueMessages = redisTemplate.opsForZSet().rangeByScore(MqConst.MQ_DELAY_KEY, 0, now, 0, 50);
        if (dueMessages == null || dueMessages.isEmpty()) {
            return;
        }
        for (Object obj : dueMessages) {
            Long removed = redisTemplate.opsForZSet().remove(MqConst.MQ_DELAY_KEY, obj);
            if (removed != null && removed > 0) {
                redisTemplate.opsForList().leftPush(MqConst.MQ_QUEUE_KEY, obj);
            }
        }
    }

    private void dispatch(MqMessage mqMessage) {
        String routingKey = mqMessage.getRoutingKey();
        Object payload = mqMessage.getPayload();

        if (MqConst.ROUTING_DELETE_CART.equals(routingKey)) {
            Long userId = toLong(payload);
            if (userId != null) {
                cartInfoService.deleteCartChecked(userId);
            }
            return;
        }
        if (MqConst.ROUTING_MINUS_STOCK.equals(routingKey)) {
            String orderNo = payload == null ? null : payload.toString();
            if (!StringUtils.isEmpty(orderNo)) {
                skuInfoService.minusStock(orderNo);
            }
            return;
        }
        if (MqConst.ROUTING_PAY_SUCCESS.equals(routingKey)) {
            String orderNo = payload == null ? null : payload.toString();
            if (!StringUtils.isEmpty(orderNo)) {
                orderInfoService.orderPay(orderNo);
            }
            return;
        }
        if (MqConst.ROUTING_GOODS_UPPER.equals(routingKey)) {
            Long skuId = toLong(payload);
            if (skuId != null) {
                skuService.upperSku(skuId);
            }
            return;
        }
        if (MqConst.ROUTING_GOODS_LOWER.equals(routingKey)) {
            Long skuId = toLong(payload);
            if (skuId != null) {
                skuService.lowerSku(skuId);
            }
            return;
        }

        log.warn("Unhandled mq routingKey={}, exchange={}, payloadType={}",
                routingKey,
                mqMessage.getExchange(),
                payload == null ? "null" : payload.getClass().getName());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

