package com.sky.annotation;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 12:20
 **/

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解,用来标识某个方法需要进行公共字段自动填充处理
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {

    //数据库操作类型:UPDATE INSERT
    OperationType value();
}
