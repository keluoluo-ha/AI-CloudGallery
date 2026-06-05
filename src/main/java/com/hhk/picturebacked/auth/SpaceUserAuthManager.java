package com.hhk.picturebacked.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.hhk.picturebacked.auth.model.SpaceUserAuthConfig;
import com.hhk.picturebacked.auth.model.SpaceUserRole;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.SpaceUser;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.SpaceRoleEnum;
import com.hhk.picturebacked.model.enums.SpaceTypeEnum;
import com.hhk.picturebacked.service.SpaceUserService;
import com.hhk.picturebacked.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户团队空间权限认证
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    public UserService userService;
    @Resource
    public SpaceUserService spaceUserService;
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static  {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);

    }

    /**
     * 根据角色获取权限列表
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionByRole(String spaceUserRole){
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if(role==null){
            return new ArrayList<>();
        }
        return role.getPermissions();

    }


    /**
     * 根据空间获取对应的权限
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }


}
