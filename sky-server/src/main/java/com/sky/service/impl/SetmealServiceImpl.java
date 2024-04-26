package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Autor：林建威
 * @DateTime：2024/4/26 9:13
 **/

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    public void insert(SetmealDTO setmealDTO) {
        //数据拷贝到实体中传入数据库
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //获取insert语句生成的主键值
        Long setmealId = setmeal.getId();

        //获取传递进来的套餐菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            //setmealDish依次表示setmealDishes菜品中的元素,并将设置为当前插入的主键值
            setmealDish.setSetmealId(setmealId);
        }
        //批量将套餐和菜品插入到套餐菜品表中
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 启售或禁售套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = new Setmeal();
        //启售套餐时,需要判断套餐中的菜品是否为启售状态
        if (status == StatusConstant.ENABLE) {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            //判断根据套餐id查询菜品返回的结果是不为空且大小大于0
            if (dishList != null && dishList.size() > 0) {
                for (Dish dish : dishList) {
                    //dish依次表示dishList列表中的每一个元素
                    if (StatusConstant.DISABLE == dish.getStatus()) {
                        //如果菜品的状态为禁售状态,则抛出异常
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }
        setmeal.setStatus(status);
        setmeal.setId(id);
        setmealMapper.update(setmeal);
    }

    /**
     * 根据id查询id
     * @param id
     */
    @Override
    public SetmealVO getById(Long id) {
        //根据id获取套餐信息
        Setmeal setmeal = setmealMapper.getById(id);
        //根据套餐id查询对应的菜品
        List<SetmealDish> setmealDish = setmealDishMapper.getBySetmealId(id);
        //封装查询结果
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDish);
        return setmealVO;
    }
}
