package com.sky.task;

/**
 * @Autor：林建威
 * @DateTime：2024/5/5 9:18
 **/

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class MyTask {
    /**
     * 定时任务 每隔5秒触发一次
     */

    // @Scheduled(cron = "0/5 * * * * ?")
    public void excuseTask() {
        log.info("定时任务开始执行:{}", new Date());
    }
}
