package cn.itedus.ssyx.order.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.enums.OrderStatus;
import cn.itedus.ssyx.enums.ProcessStatus;
import cn.itedus.ssyx.model.order.OrderInfo;
import cn.itedus.ssyx.order.mapper.OrderInfoMapper;
import cn.itedus.ssyx.order.service.OrderInfoService;
import cn.itedus.ssyx.vo.order.OrderDeliverVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Api(tags = "Order Admin API")
@RestController
@RequestMapping("/admin/order/orderInfo")
public class OrderInfoAdminController {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ApiOperation("List orders by page")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long limit,
                        @RequestParam(required = false) String outTradeNo,
                        @RequestParam(required = false) String orderStatus,
                        @RequestParam(required = false) Long wareId,
                        @RequestParam(required = false) String receiver,
                        @RequestParam(required = false) String createTimeBegin,
                        @RequestParam(required = false) String createTimeEnd) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(outTradeNo), OrderInfo::getOrderNo, outTradeNo);
        wrapper.eq(wareId != null, OrderInfo::getWareId, wareId);
        wrapper.ge(StringUtils.hasText(createTimeBegin), OrderInfo::getCreateTime, createTimeBegin);
        wrapper.le(StringUtils.hasText(createTimeEnd), OrderInfo::getCreateTime, createTimeEnd);
        if (StringUtils.hasText(orderStatus)) {
            wrapper.eq(OrderInfo::getOrderStatus, OrderStatus.valueOf(orderStatus));
        }
        if (StringUtils.hasText(receiver)) {
            wrapper.and(item -> item.like(OrderInfo::getReceiverName, receiver)
                    .or().like(OrderInfo::getReceiverPhone, receiver));
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);

        IPage<OrderInfo> pageModel = orderInfoMapper.selectPage(new Page<>(page, limit), wrapper);
        for (OrderInfo orderInfo : pageModel.getRecords()) {
            fillOrderStatus(orderInfo);
        }
        return Result.ok(pageModel);
    }

    @ApiOperation("Get order detail")
    @GetMapping("get/{orderId}")
    public Result get(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderInfoById(orderId));
    }

    @ApiOperation("List delivery summary")
    @GetMapping("findReceiveList/{wareId}/{date}")
    public Result findReceiveList(@PathVariable String wareId, @PathVariable String date) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select o.leader_id leaderId, max(o.leader_name) leaderName, max(o.leader_phone) leaderPhone, ")
                .append("max(o.take_name) takeName, coalesce(sum(oi.sku_num), 0) skuNum, ")
                .append("max(od.driver_name) driverName, max(od.driver_phone) driverPhone, ")
                .append("max(od.deliver_date) deliverDate, max(od.create_time) createTime, coalesce(max(od.status), 0) deliverStatus ")
                .append("from order_info o ")
                .append("left join order_item oi on oi.order_id = o.id and oi.is_deleted = 0 ")
                .append("left join order_deliver od on od.leader_id = o.leader_id and od.is_deleted = 0 ")
                .append("and date(od.deliver_date) = date(o.create_time) ")
                .append("where o.is_deleted = 0 and o.order_status <> -1 ");
        appendWareAndDate(sql, params, wareId, date, "o");
        sql.append(" group by o.leader_id order by max(o.create_time) desc");
        return Result.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @ApiOperation("List delivery detail by leader")
    @GetMapping("findLeaderReceiveList/{leaderId}/{date}")
    public Result findLeaderReceiveList(@PathVariable Long leaderId, @PathVariable String date) {
        List<Object> params = new ArrayList<>();
        params.add(leaderId);
        StringBuilder sql = new StringBuilder();
        sql.append("select oi.sku_name skuName, sum(oi.sku_num) skuNum ")
                .append("from order_item oi ")
                .append("inner join order_info o on o.id = oi.order_id ")
                .append("where oi.is_deleted = 0 and o.is_deleted = 0 and o.order_status <> -1 and o.leader_id = ? ");
        if (hasValue(date)) {
            sql.append("and date(o.create_time) = ? ");
            params.add(date);
        }
        sql.append("group by oi.sku_id, oi.sku_name order by oi.sku_name");
        return Result.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @ApiOperation("Confirm delivery")
    @PostMapping("deliver")
    public Result deliver(@RequestBody OrderDeliverVo orderDeliverVo) {
        Date deliverDate = orderDeliverVo.getDeliverDate() == null ? new Date() : orderDeliverVo.getDeliverDate();
        String dateText = new SimpleDateFormat("yyyy-MM-dd").format(deliverDate);
        Long leaderId = orderDeliverVo.getLeaderId();

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from order_deliver where is_deleted = 0 and leader_id = ? and date(deliver_date) = ?",
                new Object[]{leaderId, dateText},
                Integer.class);
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "update order_deliver set driver_id = ?, driver_name = ?, driver_phone = ?, status = 1, update_time = now() where is_deleted = 0 and leader_id = ? and date(deliver_date) = ?",
                    orderDeliverVo.getDriverId(), orderDeliverVo.getDriverName(), orderDeliverVo.getDriverPhone(), leaderId, dateText);
        } else {
            jdbcTemplate.update(
                    "insert into order_deliver(deliver_date, leader_id, driver_id, driver_name, driver_phone, status, create_time, update_time, is_deleted) values(?, ?, ?, ?, ?, 1, now(), now(), 0)",
                    deliverDate, leaderId, orderDeliverVo.getDriverId(), orderDeliverVo.getDriverName(), orderDeliverVo.getDriverPhone());
        }

        OrderInfo update = new OrderInfo();
        update.setProcessStatus(ProcessStatus.WAITING_LEADER_TAKE);
        update.setOrderStatus(ProcessStatus.WAITING_LEADER_TAKE.getOrderStatus());
        update.setDeliveryTime(new Date());
        orderInfoMapper.update(update, new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getLeaderId, leaderId)
                .eq(OrderInfo::getOrderStatus, OrderStatus.WAITING_DELEVER)
                .apply("date(create_time) = {0}", dateText));
        return Result.ok();
    }

    private void fillOrderStatus(OrderInfo orderInfo) {
        if (orderInfo != null && orderInfo.getOrderStatus() != null) {
            orderInfo.getParam().put("orderStatusName", orderInfo.getOrderStatus().getComment());
        }
    }

    private void appendWareAndDate(StringBuilder sql, List<Object> params, String wareId, String date, String alias) {
        if (hasValue(wareId)) {
            sql.append("and ").append(alias).append(".ware_id = ? ");
            params.add(Long.valueOf(wareId));
        }
        if (hasValue(date)) {
            sql.append("and date(").append(alias).append(".create_time) = ? ");
            params.add(date);
        }
    }

    private boolean hasValue(String value) {
        return StringUtils.hasText(value) && !"undefined".equals(value) && !"null".equals(value);
    }
}
