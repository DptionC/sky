package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/29 15:42
 **/

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Slf4j
@Api(tags = "C端-分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询分类接口
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类接口")
    public Result<List<Category>> list(Integer type) {
        log.info("查询分类:{}", type);
        List<Category> categoryList = categoryService.queryByType(type);
        return Result.success(categoryList);

    }
}
