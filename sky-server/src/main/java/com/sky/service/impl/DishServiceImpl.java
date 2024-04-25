package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 15:47
 **/

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Transactional //确保插入数据的一致性，添加事务注解
    @Override
    public void insertWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //1.向菜品表插入一条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        //获取传进来的口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //判断口味表是否为空
        if (flavors != null && flavors.size() > 0) {
            //遍历传递进来的口味信息，对里面的dishId进行赋值
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            //2.向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        //持久层处理结果使用DishVO这个泛型来封装返回给前端渲染,用于在视图层展示菜品信息
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional //添加事务注解,确保数据的一致性
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断是否存在起售中的菜品
        for (Long id : ids) {
            //依次遍历传递进来的id对应的菜品信息
            Dish dish = dishMapper.getById(id);
            //根据获取到的对应菜品信息进行状态判断
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品为启售状态,不能进行删除,抛出错误
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishId(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            //当前菜品已经关联套餐,不能进行删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的数据
        /*for (Long id : ids) {
            //根据id删除对应的菜品数据
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deletByDishId(id);
        }*/
        //优化
        //sql: delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);
        //sql: delete from dish_flavor where dish_id in (?,?,?)
        dishFlavorMapper.deletByDishIds(ids);

    }

    /**
     * 启售或停售菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }

    @Override
    public DishVO getById(Long id) {
        DishVO dishId = dishMapper.getForId(id);
        dishId.getFlavors();
        return dishId;
    }
}
