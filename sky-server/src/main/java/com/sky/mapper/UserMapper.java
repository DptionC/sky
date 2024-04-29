package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
