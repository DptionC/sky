package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Autor：林建威
 * @DateTime：2024/4/26 9:12
 **/
@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private DishService dishService;


    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}",setmealDTO);
        setmealService.insert(setmealDTO);
        return Result.success();
     }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping
    @ApiOperation("根据id查询套餐")
    public Result getById(Long id) {
        log.info("根据id查询套餐:{}",id);
        SetmealVO setmealVO =setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /**
     * 启售或停售套餐
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启售或停售套餐")
    public Result startOrStop(@PathVariable Integer status,Long id) {
        log.info("启售或停售套餐:{},{}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
