package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/30 12:39
 **/

public interface ShoppingCartService {

    /**
     * 添加商品到购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看购物车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();

    /**
     * 减少购物车中菜品或套餐数量
     * @param shoppingCartDTO
     * @return
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
