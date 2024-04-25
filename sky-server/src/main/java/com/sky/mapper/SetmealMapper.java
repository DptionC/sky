package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 9:51
 **/

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);
}
