package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        //Duplicate entry 'zhangsan' for key 'employee.idx_username',使用getMessage捕获错误信息
        String message = ex.getMessage();
        //判断是否包含键值对
        if (message.contains("Duplicate entry")) {
            //如果包含,则通过空格分割
            String[] split = message.split(" ");
            //根据分割后的索引下标获取重复的用户名
            String username = split[2];
            //进行字符串拼接
            String msg = username + MessageConstant.ALREADY_EXISTS;
            //封装结果返回
            return Result.error(msg);
        }else{
            //如果是其他问题,则返回未知错误
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
