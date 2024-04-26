package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

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
}
