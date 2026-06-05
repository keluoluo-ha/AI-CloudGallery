package com.hhk.picturebacked.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.auth.StpKit;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.constant.UserConstant;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.model.dto.user.UserQueryRequest;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.UserRoleEnums;
import com.hhk.picturebacked.model.vo.UserLoginVO;
import com.hhk.picturebacked.model.vo.UserVO;
import com.hhk.picturebacked.service.UserService;
import com.hhk.picturebacked.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 30674
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-04-14 08:44:12
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Resource
    public UserMapper userMapper;
    @Override
    public String getEncryptPassword(String password){
        final String SALT="hhk";
        return DigestUtils.md5DigestAsHex((SALT+password).getBytes());
    }

    @Override
    public UserLoginVO UserLogin(String userAccount, String UserPassword, HttpServletRequest request) {
        //1.校验
        if(userAccount==null||UserPassword==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户名或密码不能为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户过短");
        }
        if(UserPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码过短");
        }
        //2.密码加密
        String encryptPassword = getEncryptPassword(UserPassword);
        //2.查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户或密码不存在");
        }
        //记录用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        //记录用户登录到Sa-token 便于空间鉴权使用，注意保证该用户信息与SpringSession的国旗时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE,user);
        return this.getLoginVo(user);

    }

    @Override
    public UserLoginVO getLoginVo(User user) {
        UserLoginVO userLoginVO = new UserLoginVO();
        if(user==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        BeanUtils.copyProperties(user,userLoginVO);
        return userLoginVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User CurrentUser= (User) attribute;
        if(CurrentUser==null||CurrentUser.getId()==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long id = CurrentUser.getId();
        CurrentUser = this.getById(id);
        if(CurrentUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return CurrentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(attribute==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVo(User user) {
        if(user==null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest request) {


        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数不能为空");
        }
        Long id = request.getId();
        String userName = request.getUserName();
        String userAccount = request.getUserAccount();
        String userProfile = request.getUserProfile();
        String userRole = request.getUserRole();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        userQueryWrapper.eq(StrUtil.isNotBlank(userRole),"userRole",userRole);

        userQueryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        userQueryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        userQueryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        userQueryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return userQueryWrapper;

    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnums.ADMIN.getValue().equals(user.getUserRole());
    }



    @Override
    public Long UserRegister(String userAccount, String UserPassword, String checkPassword) {
        //1.校验
      if(StrUtil.hasBlank(userAccount,UserPassword,checkPassword)){
          throw new BusinessException(ErrorCode.PARAMS_ERROR);
      }
      if(userAccount.length()<4){
          throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数过短");
      }
      if(UserPassword.length()<8||checkPassword.length()<8){
          throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数过短");
      }
      if(!UserPassword.equals(checkPassword)){
          throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不一致");
      }
      //2.检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        Long selectCount = userMapper.selectCount(queryWrapper);
        if(selectCount>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户不能重复");
        }
        //3.加密
        String encryptPassword = getEncryptPassword(UserPassword);
        //4.插入数据
        User RegisterUser = new User();
        RegisterUser.setUserAccount(userAccount);
        RegisterUser.setUserPassword(encryptPassword);
        RegisterUser.setUserRole(UserRoleEnums.USER.getValue());
        RegisterUser.setUserName("无名");
        //5.增加user
        boolean save = this.save(RegisterUser);// 等于userMapper.insert(RegisterUser)
        if(!save){
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return RegisterUser.getId();

    }

}




