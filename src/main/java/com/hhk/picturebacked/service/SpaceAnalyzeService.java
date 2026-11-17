package com.hhk.picturebacked.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.dto.space.Analyze.*;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.spaceAnalyze.*;

import java.util.List;

/**
* @author 30674
* @description 针对空间分析
* @createDate 2026-04-27 15:45:53
*/
public interface SpaceAnalyzeService extends IService<Space> {


    /**
     * 空间资源使用分析
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse SpaceAnalyzeUsage(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,User loginUser);

    /**
     * 空间分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> SpaceAnalyzeCategory(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> SpaceAnalyzeTag(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取用户行为分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,User loginUser);

    /**
     * 查询前n个空间
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

    /**
     * 素材覆盖率分析
     * 按平台维度统计 SKU 素材齐备率
     *
     * @param request 分析请求
     * @param loginUser 登录用户
     * @return 各平台覆盖率
     */
    List<SpaceCoverageAnalyzeResponse> getSpaceCoverageAnalyze(SpaceCoverageAnalyzeRequest request, User loginUser);

    /**
     * 素材缺口分析
     * 按平台维度列出缺失素材的 SKU 明细
     *
     * @param request 分析请求
     * @param loginUser 登录用户
     * @return 缺口明细
     */
    List<SpaceMissingAnalyzeResponse> getSpaceMissingAnalyze(SpaceMissingAnalyzeRequest request, User loginUser);
}
