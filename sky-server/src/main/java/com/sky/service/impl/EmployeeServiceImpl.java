package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordEditFailedException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void addUser(EmployeeDTO employeeDTO) {
        //将接收前端提交的数据,转成实体类
        Employee employee = new Employee();
        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);
        //设置账号的默认状态,默认正常是1表示正常,0表示禁用
        employee.setStatus(StatusConstant.ENABLE);
        //设置账号的默认密码,使用MD5加密,默认密码123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //设置当前记录的创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //设置当前记录创建人id和修改人id
        //通过threadLocal获取当前线程所对应的局部变量的值
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        //调用持久层(Dao)Mapper.
        employeeMapper.addUser(employee);

    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //使用PageHelper插件,开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        //调用持久层(Dao)既Mapper,并且遵循PageHelp原则,返回值为page<E>
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        //获取总记录数
        long total = page.getTotal();
        //获取当前页数据集合
        List<Employee> records = page.getResult();
        //将total和records封装到PageResult返回
        return new PageResult(total,records);
    }

    /**
     * 启用或禁用员工
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, long id) {
        //为了修改的有更好的通用性,创建一个实体类
        Employee employee = new Employee();
        employee.setStatus(status);
        employee.setId(id);
        //调用Mapper层进行数据库处理
        employeeMapper.update(employee);
    }

    /**
     * 根据ID查询员工
     * @param id
     * @return
     */
    @Override
    public Employee getById(long id) {
        //调用Mapper进行数据处理
        Employee employee = employeeMapper.getById(id);
        //加强密码的安全性,非明文显示
        employee.setPassword("*****");
        return employee;
    }

    /**
     * 修改员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //数据拷贝
        BeanUtils.copyProperties(employeeDTO,employee);
        //更新修改时间
        employee.setUpdateTime(LocalDateTime.now());
        //设置修改用户id
        employee.setUpdateUser(BaseContext.getCurrentId());
        //调用持久层
        employeeMapper.update(employee);
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        //获取当前用户id,通过ThreadLocal来获取当前线程所对应的线程局部变量的值
        Long currentId = BaseContext.getCurrentId();
        //根据当前id进行数据库查询对应的员工密码信息
        Employee currentUser = employeeMapper.getById(currentId);
        //判断旧密码hash值是否相同,如果不同则抛出旧密码错误提示,StandardCharsets.UTF_8确保所有字符都能正确处理
        if (!currentUser.getPassword().equals(DigestUtils.md5DigestAsHex(passwordEditDTO.getOldPassword().getBytes(StandardCharsets.UTF_8)))) {
            throw new PasswordEditFailedException("旧密码错误!");
        }

        //判断新密码是否符合要求
        if (!isValidPassword(passwordEditDTO.getNewPassword())) {
            throw new PasswordEditFailedException("新密码格式不正确,密码长度应在6-20位，且包含数字和字母（区分大小写）!");
        }
        //如果满足,则更新密码
        currentUser.setPassword(DigestUtils.md5DigestAsHex(passwordEditDTO.getNewPassword().getBytes(StandardCharsets.UTF_8)));
        employeeMapper.update(currentUser);
    }

    private boolean isValidPassword(String password) {
        // 密码长度应在6-20位之间
        if (password.length() < 6 || password.length() > 20) {
            return false;
        }
        // 密码包含数字和字母（区分大小写）
        boolean hasDigit = false;
        boolean hasLowercaseLetter = false;
        boolean hasUppercaseLetter = false;
        //增强for循环
        for (char ch : password.toCharArray()) {
            //ch依次表示password字符串数组的元素
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (Character.isLowerCase(ch)) {
                hasLowercaseLetter = true;
            } else if (Character.isUpperCase(ch)) {
                hasUppercaseLetter = true;
            }
            // 如果已经包含数字、小写字母和大写字母，则密码格式正确
            if (hasDigit && hasLowercaseLetter && hasUppercaseLetter) {
                return true;
            }
        }
        return false;
    }

}
