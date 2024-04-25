package com.sky.controller.upload;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.AuthorizationScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 14:44
 **/

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "上传相关接口")
public class UploadController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) { //file需要与接口文档参数名一致
        log.info("文件上传:{}",file);

        try {
            //获取文件原始名
            String originalFilename = file.getOriginalFilename();
            //截取源文件名后缀，abdsd.jpg
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //使用UUID与后缀进行拼接
            String fileName = UUID.randomUUID().toString() + extension;
            String filePath = aliOssUtil.upload(file.getBytes(), fileName);
            //最终返回上传文件路径
            return Result.success(filePath);
        } catch (IOException e) {
            log.info("文件上传失败:{}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
