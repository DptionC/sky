package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/30 16:39
 **/

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单详细信息
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);
}
