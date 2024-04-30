package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/30 16:38
 **/

@Mapper
public interface OrderMapper {
    /**
     * 插入数据
     * @param orders
     */
    void insert(Orders orders);

}
