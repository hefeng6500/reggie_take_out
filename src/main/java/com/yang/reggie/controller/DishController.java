package com.yang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.reggie.common.R;
import com.yang.reggie.dto.DishDto;
import com.yang.reggie.entity.Category;
import com.yang.reggie.entity.Dish;
import com.yang.reggie.entity.DishFlavor;
import com.yang.reggie.service.CategoryService;
import com.yang.reggie.service.DishFlavorService;
import com.yang.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

  @Autowired
  private RedisTemplate redisTemplate;
  @Autowired
  private DishService dishService;

  @Autowired
  private DishFlavorService dishFlavorService;

  @Autowired
  private CategoryService categoryService;
  private int newState;

  /**
   * 新增菜品
   *
   * @param dishDto
   * @return
   */
  @PostMapping
  public R<String> save(@RequestBody DishDto dishDto) {
    log.info(dishDto.toString());

    // 清理缓存所有菜品数据，不推荐
    // Set keys = redisTemplate.keys("dish_*");
    // redisTemplate.delete(keys);

    // 清理某个分类下面的缓存,推荐，精确清理
    String key = "dish_" + dishDto.getCategoryId() + "_1";

    redisTemplate.delete(key);

    dishService.saveWithFlavor(dishDto);

    return R.success("新增菜品成功");
  }

  /**
   * 菜品信息分页查询
   *
   * @param page
   * @param pageSize
   * @param name
   * @return
   */
  @GetMapping("/page")
  public R<Page> page(int page, int pageSize, String name) {

    // 构造分页构造器对象
    Page<Dish> pageInfo = new Page<>(page, pageSize);
    Page<DishDto> dishDtoPage = new Page<>();

    // 条件构造器
    LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
    // 添加过滤条件
    queryWrapper.like(name != null, Dish::getName, name);
    // 添加排序条件
    queryWrapper.orderByDesc(Dish::getUpdateTime);

    // 执行分页查询
    dishService.page(pageInfo, queryWrapper);

    // 对象拷贝
    BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

    List<Dish> records = pageInfo.getRecords();

    List<DishDto> list = records.stream().map((item) -> {
      DishDto dishDto = new DishDto();

      BeanUtils.copyProperties(item, dishDto);

      Long categoryId = item.getCategoryId();// 分类id
      // 根据id查询分类对象
      Category category = categoryService.getById(categoryId);

      if (category != null) {
        String categoryName = category.getName();
        dishDto.setCategoryName(categoryName);
      }
      return dishDto;
    }).collect(Collectors.toList());

    dishDtoPage.setRecords(list);

    return R.success(dishDtoPage);
  }

  /**
   * 根据id查询菜品信息和对应的口味信息
   *
   * @param id
   * @return
   */
  @GetMapping("/{id}")
  public R<DishDto> get(@PathVariable Long id) {

    DishDto dishDto = dishService.getByIdWithFlavor(id);

    return R.success(dishDto);
  }

  /**
   * 修改菜品
   *
   * @param dishDto
   * @return
   */
  @PutMapping
  public R<String> update(@RequestBody DishDto dishDto) {
    log.info(dishDto.toString());

    String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
    redisTemplate.delete(key);

    dishService.updateWithFlavor(dishDto);

    return R.success("新增菜品成功");
  }


  /**
   * 根据条件查询对应的菜品数据
   *
   * @param dish
   * @return
   */
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/
  @GetMapping("/list")
  public R<List<DishDto>> list(Dish dish) {
    log.info("dish:{}", dish);

    // 先从缓存中获取缓存数据
    List<DishDto> dishDtos = null;

    // 动态构造 key
    String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

    dishDtos = (List<DishDto>) redisTemplate.opsForValue().get(key);

    if(dishDtos != null) {
      return R.success(dishDtos);
    }

    // 条件构造器
    LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(StringUtils.isNotEmpty(dish.getName()), Dish::getName, dish.getName());
    queryWrapper.eq(null != dish.getCategoryId(), Dish::getCategoryId, dish.getCategoryId());
    // 添加条件，查询状态为1（起售状态）的菜品
    queryWrapper.eq(Dish::getStatus, 1);
    queryWrapper.orderByDesc(Dish::getUpdateTime);


    // 查询对应菜品的分类和口味信息
    List<Dish> dishs = dishService.list(queryWrapper);

    dishDtos = dishs.stream().map(item -> {
      DishDto dishDto = new DishDto();
      BeanUtils.copyProperties(item, dishDto);
      Category category = categoryService.getById(item.getCategoryId());
      if (category != null) {
        dishDto.setCategoryName(category.getName());
      }
      LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<>();
      wrapper.eq(DishFlavor::getDishId, item.getId());

      dishDto.setFlavors(dishFlavorService.list(wrapper));
      return dishDto;
    }).collect(Collectors.toList());

    redisTemplate.opsForValue().set(key, dishDtos, 60, TimeUnit.MINUTES);

    return R.success(dishDtos);
  }

  /**
   * 起售停售状态切换
   *
   * @return
   */
  @PostMapping("/status/{status}")
  public R<String> toggleStatus(HttpServletRequest httpServletRequest, @PathVariable Integer status, @RequestParam List<Long> ids) {


    LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();

    lambdaQueryWrapper.in(ids != null, Dish::getId, ids);

    List<Dish> list = dishService.list(lambdaQueryWrapper);

    for (Dish dish : list) {
      if (dish != null) {
        dish.setStatus(status);

        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        redisTemplate.delete(key);

        dishService.updateById(dish);
      }
    }

    return R.success(status == 1 ? "启售成功" : "停售成功");
  }


  /**
   * 删除菜品
   *
   * @param httpServletRequest
   * @param ids
   * @return
   */
  @DeleteMapping
  public R<String> deleteDish(HttpServletRequest httpServletRequest, @RequestParam List<Long> ids) {
    LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();

    lambdaQueryWrapper.in(ids != null, Dish::getId, ids);

    List<Dish> list = dishService.list(lambdaQueryWrapper);

    for (Dish dish : list) {
      if (dish != null) {
        int status = dish.getStatus();

        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        redisTemplate.delete(key);

        if (status == 1) {
          // 启售中
          return R.error(list.size() == 1 ? "删除失败！该商品正在启售中，不可删除！" : "删除失败！存在启售商品！");
        }
        dishService.removeByIds(ids);
      }
    }

    return R.success("删除成功！");
  }
}

