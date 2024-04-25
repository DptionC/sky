package com.sky.controller.category;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/24 22:05
 **/

/*
    分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<String> addClassify(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类:{}",categoryDTO);
        //调用Service层进行业务逻辑处理
        categoryService.addClassify(categoryDTO);
        return Result.success();
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分页查询内容:{}",categoryPageQueryDTO);
        //调用service层进行业务逻辑处理
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 启用或禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用分类")
    public Result<String> stopOrStop(@PathVariable Integer status, Long id) {
        log.info("当前id:{},状态为:{}", id, status);
        //调用Service层进行业务逻辑处理
        categoryService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据id删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除分类")
    public Result<String> deleteById(Long id) {
        log.info("删除的分类id:{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> queryByType(Integer type) {
        log.info("查询类型为:{}",type);
        List<Category> categoryList = categoryService.queryByType(type);
        return Result.success(categoryList);
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO) {
        log.info("修改的分类为:{}",categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }
 }
