package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @Autor：林建威
 * @DateTime：2024/4/29 12:48
 **/

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //定义微信服务接口定制
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override

    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务,当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type", "authorization_code");
        //因为返回结果是json格式
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        //在json格式里面解析出openid
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");

        //判断openid是否为空,如果为空表示登录失败,抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //根据openid判断当前用户是否为新用户,并将结果用User实体类返回
        User user = userMapper.getByOpendId(openid);
        //如果是新用户,自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    //这里因为user实体类中只有创建时间,没有创建人和更新时间等方法,如果用自动公共字段填充会出现异常.
                    //所以这里手动设置创建时间
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回这个用户对象
        return user;
    }
}
