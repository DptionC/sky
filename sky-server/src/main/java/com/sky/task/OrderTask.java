package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/5/5 9:25
 **/

/**
 * 定时任务类,定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ?") //每分钟触发一次
    public void processTimeoutOrder() {
        log.info("定时处理超时订单:{}", LocalDateTime.now());
        //获取当前时间-15分钟的时间
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //select * from orders where status = ? and order_time < (当前时间-15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT,time);
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时,自动取消!");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理订单一直处于派送中的方法
     */
    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发一次
    public void processDeliveringOrder() {
        log.info("定时处理派送中订单:{}", LocalDateTime.now());
        //获取上一个当前时间的前一个时间,@Scheduled注解就会自动在每天的凌晨一点进行执行
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        //为什么要获取当前时间的前一个小时,因为要到数据中查询数据,要传参
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
