package com.sky.config;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 15:04
 **/

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建AliOssUtil对象
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean //将标注的方法产生一个bean对象，并交给spring容器进行管理
    @ConditionalOnMissingBean //当容器中不存在当前bean对象时，才会创建当前方法的bean对象
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传工具类对象:{}",aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                              aliOssProperties.getAccessKeyId(),
                              aliOssProperties.getAccessKeySecret(),
                              aliOssProperties.getBucketName());
    }
}
