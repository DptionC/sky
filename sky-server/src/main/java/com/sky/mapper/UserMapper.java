package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * @Autor：林建威
 * @DateTime：2024/4/29 13:10
 **/

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户是否存在
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpendId(String openid);

    /**
     * 插入新用户
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户信息
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 指定时间范围统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
