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
}
