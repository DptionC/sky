package com.sky.service;

import com.sky.dto.ShoppingCartDTO;

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
}
