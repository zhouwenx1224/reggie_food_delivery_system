package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据：{}",shoppingCart);

        //设置用户ID，置顶当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();;
        shoppingCart.setUserId(currentId);

        //查询当前菜品或者套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        if (dishId != null) {
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //如果已经存在，就在原来数量基础上加一
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
        if (cartServiceOne != null ){
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number+1);
            shoppingCartService.updateById(cartServiceOne);
        }else{
            //如果不存在，添加到购物车，数量默认是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车...");
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //只查询当前用户ID的购物车
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        //代表数量减少的是菜品数量
        if (dishId != null) {
            //通过dishId查出购物车菜品数据
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
            ShoppingCart dishCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据的数量-1
            dishCart.setNumber(dishCart.getNumber() - 1);
            Integer currentNum = dishCart.getNumber();
            //然后判断
            if (currentNum > 0) {
                //大于0则更新
                shoppingCartService.updateById(dishCart);
            } else if (currentNum == 0) {
                //小于0则删除
                shoppingCartService.removeById(dishCart.getId());
            }
            return R.success(dishCart);
        }

        if (setmealId != null) {
            //通过setmealId查询购物车套餐数据
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
            ShoppingCart setmealCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据的数量-1
            setmealCart.setNumber(setmealCart.getNumber() - 1);
            Integer currentNum = setmealCart.getNumber();
            //然后判断
            if (currentNum > 0) {
                //大于0则更新
                shoppingCartService.updateById(setmealCart);
            } else if (currentNum == 0) {
                //等于0则删除
                shoppingCartService.removeById(setmealCart.getId());
            }
            return R.success(setmealCart);
        }
        return R.error("系统繁忙，请稍后再试");
    }

    @DeleteMapping("/clean")
    public R<String> clean() {
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        Long userId = BaseContext.getCurrentId();
        lambdaQueryWrapper.eq( userId != null,ShoppingCart::getUserId,userId);

        shoppingCartService.remove(lambdaQueryWrapper);

        return R.success("成功情况购物车");
    }

}
