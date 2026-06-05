package com.hhk.picturebacked.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.dto.space.SpaceAddRequest;
import com.hhk.picturebacked.model.dto.space.SpaceQueryRequest;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 30674
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-04-27 15:45:53
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param addRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest addRequest,User loginUser);

    /**
     * 空间数据校验
     * @param Space
     */
    void validSpace(Space Space,boolean add);

    /**
     * 查询wrapper
     * @param request
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest request);

    /**
     *
     *获得SpaceVO(单条)
     * @param Space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space Space, HttpServletRequest request);
    /**
     * 获取分页空间列表
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 空间审核
     * @param SpaceReviewRequest
     * @param loginUser
     */
//    void doSpaceReview(SpaceReviewRequest SpaceReviewRequest, User loginUser);


    /**
     * 自动填入审核数据
     * @param Space
     */
    void fillSpaceBySpaceLevel(Space Space);


    void checkSpaceAuth(User loginUser,Space space);


}
