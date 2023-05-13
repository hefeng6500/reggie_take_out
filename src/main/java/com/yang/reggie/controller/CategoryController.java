package com.yang.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.reggie.common.R;
import com.yang.reggie.entity.Category;
import com.yang.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
  @Autowired
  private CategoryService categoryService;


  /**
   * 新增菜品分类
   *
   * @param request
   * @param category
   * @return
   */
  @PostMapping
  public R<String> save(HttpServletRequest request, @RequestBody Category category) {

    categoryService.save(category);
    return R.success("新增成功！");
  }


  /**
   * 分页查询菜品分类
   *
   * @param page
   * @param pageSize
   * @return
   */
  @GetMapping("/page")
  public R<Page<Category>> queryCategory(int page, int pageSize) {

    log.info(page + ":" + pageSize);

    // 构造分页器
    Page<Category> pageInfo = new Page<>(page, pageSize);

    // 条件构造器
    LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();

    // 添加排序条件， 根据 sort 进行升序排序
    lambdaQueryWrapper.orderByAsc(Category::getSort);

    categoryService.page(pageInfo, lambdaQueryWrapper);

    return R.success(pageInfo);
  }

  /**
   * 更新菜品分类信息
   *
   * @param category
   * @return
   */
  @PutMapping
  public R<String> update(@RequestBody Category category) {
    log.info("更新分类信息" + category.getName() + "id: " + category.getId());

    categoryService.updateById(category);

    return R.success("更新成功！");
  }

  /**
   * 删除菜品分类信息
   *
   * @param request
   * @param id
   * @return
   */
  @DeleteMapping
  public R<String> deleteCategory(HttpServletRequest request, Long id) {

    // Mybatis Plus 提供的 removeById
    // categoryService.removeById(id);

    try {
      categoryService.remove(id);
    }catch (Exception exception) {
      System.out.println("exception: " + exception.getMessage());
      String msg = exception.getMessage();

      return R.error(msg);
    }

    log.info("删除菜品分类信息：" + id);

    return R.success("删除成功！");
  }

  /**
   * 根据条件查询分类数据
   * @param category
   * @return
   */
  @GetMapping("/list")
  public R<List<Category>> list(Category category){
    //条件构造器
    LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
    //添加条件
    queryWrapper.eq(category.getType() != null,Category::getType,category.getType());
    //添加排序条件
    queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

    List<Category> list = categoryService.list(queryWrapper);
    return R.success(list);
  }
}
