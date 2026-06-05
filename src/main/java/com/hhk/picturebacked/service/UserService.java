package com.hhk.picturebacked.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.model.dto.user.UserQueryRequest;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.UserLoginVO;
import com.hhk.picturebacked.model.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 30674
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-04-14 08:44:12
*/

public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param UserPassword
     * @param checkPassword
     * @return
     */
    Long UserRegister(String userAccount, String UserPassword,String checkPassword);


    /**
     * 密码加密
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 用户登录
     * @param userAccount
     * @param UserPassword
     * @return
     */
    UserLoginVO UserLogin(String userAccount, String UserPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    UserLoginVO getLoginVo(User user);


    /**
     * 获取当前用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 用户脱敏(管理员)
     * @param user
     * @return
     */
    UserVO getUserVo(User user);

    /**
     * 获取用户列表
     * @param userList
     * @return
     */
    List<UserVO> getUserList(List<User> userList);

    /**
     * 查询列表
     * @param request
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest request);

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);


}
