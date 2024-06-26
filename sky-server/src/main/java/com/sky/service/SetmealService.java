package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/26 9:13
 **/

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void insert(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 启售或禁售套餐
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询套餐
     * @param id
     */
    SetmealVO getById(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);

    /**
     * 删除套餐及批量删除套餐
     * @param ids
     */
    void deleteBySetmealId(List<Long> ids);

    /**
     * 根据分类id查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> getSetmealByCategoryId(Setmeal setmeal);

    /**
     * 根据套餐id查询包含菜品
     * @param setmealId
     * @return
     */
    List<DishItemVO> getDishBySetmealId(Long setmealId);
}
