package com.hhk.picturebacked.aop;


import com.hhk.picturebacked.annotation.AuthCheck;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.UserRoleEnums;
import com.hhk.picturebacked.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 权限拦截
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor (ProceedingJoinPoint joinPoint, AuthCheck authCheck)throws Throwable
    {
        String mustRole=authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //获取当前用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnums mustRoleEum = UserRoleEnums.getEnumByValue(mustRole);
        //不需要权限
        if(mustRoleEum ==null){
            return joinPoint.proceed();
        }
        //需要权限才能放行
        //获取当前用户
        UserRoleEnums userRoleEnum = UserRoleEnums.getEnumByValue(loginUser.getUserRole());
        if(userRoleEnum==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求必须有管理员权限，但用户没用管理员权限，拒绝
        if(UserRoleEnums.ADMIN.equals(mustRoleEum)&&!UserRoleEnums.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();

    }

}
