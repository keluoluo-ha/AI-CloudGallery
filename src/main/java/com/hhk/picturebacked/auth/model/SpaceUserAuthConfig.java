package com.hhk.picturebacked.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 *角色表(Role)
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}
