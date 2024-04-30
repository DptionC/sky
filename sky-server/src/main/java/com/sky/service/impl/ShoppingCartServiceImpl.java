package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.geom.RectangularShape;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/30 12:41
 **/

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加商品到购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //首先判断商品是否存在购物车中
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //获取当前登录用户的id
        Long userId = BaseContext.getCurrentId();
        //因为不同用户的使用不同购物车
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //如果存在，则number数量加一
        if (list != null && list.size() > 0) {
            /*
                因为是根据用户id来判断不同用户，再根据dish_id setmeal_id 或口味进行查询
                如果存在相同的商品不会去新增商品，而是修改它的数量
                因为根据上面设置的条件，最多存在两种结果，要么是不存在，要么查到之后就只有一条数据
                所以获取通过获取0索引获取第一条数据
             */
            ShoppingCart cart = list.get(0);
            //相同商品则修改数量
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }else {
            //如果不存在，则插入一条购物车数据
            //首选判断当前插入的是菜品还是套餐
            Long dishId = shoppingCart.getDishId();
            Long setmealId = shoppingCart.getSetmealId();
            if (dishId != null) {
                //本次添加到购物车中的是菜品，口味信息已经在开始的List中查询了，相同菜品不同口味的相当于不同菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                //本次添加到购物车中的是套餐
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            //同样代码抽取出来，判断处理完数据后统一插入购物车
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            //如果添加的菜品有口味，在前端发起请求已经携带到后端了，并且数据拷贝给了shoppingcart中，
            //然后再list方法中进行数据库查询，如果发现没有相同口味的数据，则当成新得菜品进行插入数据库
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前登录用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        //根据当前用户id查看当前购物车信息
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        //获取当前登录用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 减少购物车中菜品或套餐得数量
     * @param shoppingCartDTO
     * @return
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //获取当前登录用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //通过用户id获取当前购物车信息
        List<ShoppingCart> goodsList = shoppingCartMapper.list(shoppingCart);
        if (goodsList != null && goodsList.size() > 0) {
            //获取数据
            ShoppingCart cart = goodsList.get(0);
            //获取当前的菜品或套餐的数量
            Integer number = cart.getNumber();
            if (number == 1) {
                //如果当前商品数量为1，则直接删除
                shoppingCartMapper.deleteByUserId(userId);
            }else {
                //如果当前商品数量>1，则减少数量
                cart.setNumber(cart.getNumber() - 1);
                //跟新数量到数据库
                shoppingCartMapper.updateNumberById(cart);
            }
        }
    }
}
