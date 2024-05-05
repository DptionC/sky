package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Autor：林建威
 * @DateTime：2024/5/5 12:49
 **/

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //创建一个集合用来存储begin到end范围的时间数据
        List<LocalDate> dateList = new ArrayList<>();
        //设置时间的第一个数据,那就是begin
        dateList.add(begin);
        //定义一个循环,当begin=end的时候结束循环
        while (!begin.equals(end)) {
            //每次循环加一天
            begin = begin.plusDays(1);
            //将加一天后的结果添加到集合中
            dateList.add(begin);
        }

        //定义一个集合用来存储每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //date依次表示集合中的每一个元素
            //因为在数据库中的日期都是存在时分秒的,所以传参进去的时候也需要有
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//获取当天0点开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取当天23:59:59.99999结束时间
            //select sum(amount) from order where order_time > beginTime and order_time < endTime and status = complete
            //创建一个Map集合
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            //如果当天营业额为0,那么统计出来的是null,所以要进行转换成0
            turnover = turnover == null ? 0.0 : turnover; //如果是空,则返回0.0,不为空则返回自身 再赋予回自身
            turnoverList.add(turnover);
        }
        //将结果封装成TurnoverReportVO返回
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        //将dateList集合中的数据用逗号进行分割,并设置为turnoverReportVO的dataList的属性值
        turnoverReportVO.setDateList(StringUtils.join(dateList, ","));
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList, ","));
        return turnoverReportVO;
    }

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //创建一个集合用来存储begin和end的时间范围
        List<LocalDate> dateList = new ArrayList<>();
        //设置开始时间
        dateList.add(begin);
        //设置循环,当begin=end时,结束循环
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        //存放每天新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            //先对总用户数量进行统计
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);

            //在对新增用户进行统计
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            //将结果查插入响应集合中
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        //封装结果返回,并用逗号进行分隔
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间的订单数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //创建一个集合用来存储begin和end的时间范围
        List<LocalDate> dateList = new ArrayList<>();
        //设置开始时间
        dateList.add(begin);
        //设置循环,当begin=end时,结束循环
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天有效订单总数
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = getOrderCount(beginTime, endTime, null);
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }
        //获取订单总数,遍历orderCountList然后相加即可,其中Integer::sum-->等于Integer 引用sum方法
        Integer totalOrders = orderCountList.stream().reduce(Integer::sum).get();
        //获取有效订单数,同上
        Integer validOrders = validOrderCountList.stream().reduce(Integer::sum).get();
        //初始化订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrders != 0) {
            //当订单数量不为0的时候,再计算订单完成率,只需强转一个为double就行
            orderCompletionRate = validOrders.doubleValue() / totalOrders;
        }

        //将结果封装
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrders)
                .validOrderCount(validOrders)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 统计指定时间区间的销量前10数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10ReportVOResult(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        //因为根据查询结果是GoodsSalesDTO,要转换类型,使用stream流,引用GoodsSalesDTO的获取名字方法,并新建一个集合返回
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        //同理
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        //封装结果返回
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

}
