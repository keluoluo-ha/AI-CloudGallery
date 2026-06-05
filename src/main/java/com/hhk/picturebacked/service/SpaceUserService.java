package com.hhk.picturebacked.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.hhk.picturebacked.model.dto.spaceUser.SpaceUserAddRequest;
import com.hhk.picturebacked.model.dto.spaceUser.SpaceUserQueryRequest;

import com.hhk.picturebacked.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.vo.SpaceUserVO;


import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 30674
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-05-11 10:30:21
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 创建团队空间
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);


    /**
     * 查询wrapper
     * @param request
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest request);

    /**
     *
     *获得SpaceVO(单条)
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 获取分页空间列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOPage(List<SpaceUser> spaceUserList);


    /**
     * 校验空间成员
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);




}
