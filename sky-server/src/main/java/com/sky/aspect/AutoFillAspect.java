package com.sky.aspect;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 12:25
 **/

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.sql.Time;
import java.time.LocalDateTime;

/**
 * 自定义切面类,统一拦截加入了AutoFill注解的方法
 * 实现公共字段自动填充处理逻辑
 */
@Aspect
@Slf4j
@Component
public class AutoFillAspect {

    /*
        切入点
        前者锁定mapper下所有的类和方法,同时需要满足添加了@AutoFill的注解
        如果直接使用@annotation,会扫描包下全部,而加上execution就是锁定某一个扫描文件
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知,在通知中进行公共字段进行赋值
     * 在目的资源方法运行前执行
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");
        //获取当前被拦截方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解类型
        OperationType operationType = autoFill.value(); //获得数据库操作类型
        //获取当前被拦截方法的参数---实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        //要实现自动填充功能,所以实体必须要作为第一个参数,而参数的顺序根据是否有添加@AutoFill来决定
        Object entity = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据当前不同的操作类型,为对应的属性通过反射来赋值
        if (operationType == OperationType.INSERT) {
            //为4个公共字段赋值
            try {
                //参数一:方法名称 参数二:方法参数
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射为对象属性赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (operationType == OperationType.UPDATE) {
            //为2个公共字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
