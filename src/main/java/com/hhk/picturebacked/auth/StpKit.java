package com.hhk.picturebacked.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 * 添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 *
 * 用户表：用 SpaceUserAuthContext 中的 SpaceUser 对象替代
 * 角色表：用 spaceUserAuthConfig.json 中的 roles 数组替代
 * 权限表：用 SpaceUserPermissionConstant 接口替代
 * 用户角色表：用 SpaceUser 的 spaceRole 字段直接存储，不需要关联表
 * 角色权限表：用 spaceUserAuthConfig.json 中的 permissions 数组替代
 *
 * spaceUserAuthConfig.json 是角色数据的存储位置，而 SpaceUserRole.java 是角色数据的程序表示结构，它们分别处于不同的抽象层面。
 */
@Component
public class StpKit {


    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到 是User登陆体系
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
