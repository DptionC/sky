package com.sky.context;

public class BaseContext {

    //创建一个长整型的ThreadLocal集合对象
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    //创建一个设置当前线程的线程局部变量的值
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    //创建一个返回当前线程所对应的线程局部变量的值
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    //创建一个移除当前线程的线程局部变量
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
