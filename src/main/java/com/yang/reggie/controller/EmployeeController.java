package com.yang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.reggie.common.R;
import com.yang.reggie.entity.Employee;
import com.yang.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

  @Autowired
  private EmployeeService employeeService;

  /**
   * 员工登录
   *
   * @param request
   * @param employee
   * @return
   */
  @PostMapping("/login")
  public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {

    // 1、将页面提交的密码password进行md5加密处理
    String password = employee.getPassword();
    password = DigestUtils.md5DigestAsHex(password.getBytes());

    // 2、根据页面提交的用户名username查询数据库
    LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Employee::getUsername, employee.getUsername());
    Employee emp = employeeService.getOne(queryWrapper);

    // 3、如果没有查询到则返回登录失败结果
    if (emp == null) {
      return R.error("登录失败");
    }

    // 4、密码比对，如果不一致则返回登录失败结果
    if (!emp.getPassword().equals(password)) {
      return R.error("登录失败");
    }

    // 5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
    if (emp.getStatus() == 0) {
      return R.error("账号已禁用");
    }

    // 6、登录成功，将员工id存入Session并返回登录成功结果
    request.getSession().setAttribute("employee", emp.getId());
    return R.success(emp);
  }

  /**
   * 员工退出
   *
   * @param request
   * @return
   */
  @PostMapping("/logout")
  public R<String> logout(HttpServletRequest request) {
    // 清理Session中保存的当前登录员工的id
    request.getSession().removeAttribute("employee");
    return R.success("退出成功");
  }


  /**
   * @param page
   * @param pageSize
   * @param name     员工姓名
   * @return
   * @Description: 查询员工信息
   */
  @GetMapping("/page")
  public R<Page<Employee>> queryEmployee(int page, int pageSize, String name) {

    log.info(page + ":" + pageSize + ":" + name);

    // 构造分页构造器
    Page<Employee> pageInfo = new Page<>(page, pageSize);

    // 构造条件过滤器
    LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

    // 添加查询条件
    queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);

    // 添加排序条件
    queryWrapper.orderByDesc(Employee::getUpdateTime);

    employeeService.page(pageInfo, queryWrapper);

    return R.success(pageInfo);
  }

  /**
   * 根据 id 查询员工信息
   *
   * @param id
   * @return
   */
  @GetMapping("/{id}")
  public R<Employee> getById(@PathVariable Long id) {

    log.info("根据 id 查询员工信息" + id);

    Employee employee = employeeService.getById(id);

    if (employee != null) {
      return R.success(employee);
    }
    return R.error("没有该用户！");
  }


  /**
   * @param request
   * @param employee
   * @return
   * @Description 新增员工
   */
  @PostMapping
  public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
    log.info("新增员工....{}", employee);

    // 为员工设置默认的登录密码，为123456
    employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
    // 设置创建和登录时间
    employee.setCreateTime(LocalDateTime.now());
    employee.setUpdateTime(LocalDateTime.now());

    // 获取当前登录用户信息
    // Session中存储的是登录用户的id信息
    long curUserId = (long) request.getSession().getAttribute("employee");

    // 设置创建人和修改人的id
    employee.setCreateUser(curUserId);
    employee.setUpdateUser(curUserId);

    employeeService.save(employee);
    return R.success("新增员工成功");
  }

  @PutMapping
  public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
    log.info("更新员工信息，员工 id => " + employee.getId() + "姓名 => " + employee.getUsername());

    employeeService.updateById(employee);

    return R.success("更新成功！");
  }
}
