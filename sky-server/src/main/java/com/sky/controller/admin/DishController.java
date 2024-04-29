package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @Autor：林建威
 * @DateTime：2024/4/25 15:41
 **/

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品和口味
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result addDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.insertWithFlavor(dishDTO);
        //构建redis的key
        String key = "dish_" + dishDTO.getCategoryId();
        //清楚缓存
        cleanCache(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询开始:{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除的菜品:{}", ids);
        dishService.deleteBatch(ids);
        //清理所有开头为dish_*的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 启售或停售菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启售或停售菜品")
    public Result<String> startOrStop(@PathVariable Integer status,Long id) {
        log.info("启售或停售的菜品:{},{}", status, id);
        dishService.startOrStop(status, id);
        //清理所有开头为dish_*的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}", id);
        DishVO dishVo = dishService.getById(id);
        return Result.success(dishVo);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品信息:{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //清理所有开头为dish_*的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryIdToDish(Long categoryId) {
        log.info("根据分类id查询菜品:{}", categoryId);
        List<Dish> dishList = dishService.getByCategoryIdtoDish(categoryId);
        return Result.success(dishList);
    }

    /**
     * 统一清除缓存
     * @param pattern
     */
    private void cleanCache(String pattern) {
        //通过遍历获取dish_开头的key
        Set keys = redisTemplate.keys(pattern);
        //删除所有key
        redisTemplate.delete(keys);
    }
}
