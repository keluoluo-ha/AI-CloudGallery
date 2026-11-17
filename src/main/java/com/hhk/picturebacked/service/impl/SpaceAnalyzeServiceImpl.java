package com.hhk.picturebacked.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.mapper.SpaceMapper;
import com.hhk.picturebacked.model.dto.space.Analyze.*;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.AssetTypeEnum;
import com.hhk.picturebacked.model.enums.PlatformAssetRuleEnum;
import com.hhk.picturebacked.model.vo.spaceAnalyze.*;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.ProductService;
import com.hhk.picturebacked.service.SpaceAnalyzeService;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 30674
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-04-27 15:45:53
*/
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService {

    @Resource
    public UserService userService;

    @Resource
    public SpaceService spaceService;

    @Resource
    public PictureService pictureService;

    @Resource
    public ProductService productService;

    @Override
    public SpaceUsageAnalyzeResponse SpaceAnalyzeUsage(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest==null,ErrorCode.PARAMS_ERROR);
        //权限校验
       if(spaceUsageAnalyzeRequest.isQueryAll()||spaceUsageAnalyzeRequest.isQueryPublic()){
           //公共图库或者全部空间
           //只有管理员可以
          checkAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
           QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
           queryWrapper.select("picSize");
           fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest,queryWrapper);
           //数据库请求
           List<Object> pictureList = pictureService.getBaseMapper().selectObjs(queryWrapper);
           long usedSize = pictureList.stream().mapToLong(res ->
                   res instanceof Long ? (Long) res : 0).sum();
           Long usedCount = (long) pictureList.size();
           //封装响应类
           SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
           spaceUsageAnalyzeResponse.setUsedSize(usedSize);
           spaceUsageAnalyzeResponse.setUsedCount(usedCount);
           //公共图库无上限
           spaceUsageAnalyzeResponse.setCountUsageRatio(null);
           spaceUsageAnalyzeResponse.setMaxCount(null);
           spaceUsageAnalyzeResponse.setMaxSize(null);
           spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
           return spaceUsageAnalyzeResponse;
       }else {
           Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
           ThrowUtils.throwIf(spaceId==null,ErrorCode.PARAMS_ERROR);
           //权限校验
           checkAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
           //数据库请求
           Space space = this.getById(spaceId);
           ThrowUtils.throwIf(space==null,ErrorCode.PARAMS_ERROR,"空间不存在");
           SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
           //封装响应类
           response.setUsedSize(space.getTotalSize());
           response.setMaxSize(space.getMaxSize());
           // 后端直接算好百分比，这样前端可以直接展示
           double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
           response.setSizeUsageRatio(sizeUsageRatio);
           response.setUsedCount(space.getTotalCount());
           response.setMaxCount(space.getMaxCount());
           double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
           response.setCountUsageRatio(countUsageRatio);
           return response;

       }

    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> SpaceAnalyzeCategory(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest==null,ErrorCode.PARAMS_ERROR);
        //权限校验
        checkAnalyzeAuth(spaceCategoryAnalyzeRequest,loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest,queryWrapper);
        // 使用 MyBatis Plus 分组查询
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        // 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> SpaceAnalyzeTag(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("tags");
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的标签
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签并统计使用次数（扁平化）
        Map<String, Long> tagsMap = tagJsonList.stream()
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 转换为响应对象，按使用次数降序排序
        return tagsMap.entrySet().stream()
                .sorted((e1,e2)->
                    Long.compare(e2.getValue(), e1.getValue()))
                .map(entry-> new SpaceTagAnalyzeResponse(entry.getKey(),entry.getValue()))
                .collect(Collectors.toList());

    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());

        // 定义分段范围，注意使用有序 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(reslut->{
                    String period = reslut.get("period").toString();
                    long count = ((Number) reslut.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period,count);
                }).collect(Collectors.toList());


    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
// 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

// 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

// 查询结果
        return spaceService.list(queryWrapper);
    }

    /**
     * 素材覆盖率分析
     * <p>
     * 按平台维度统计「SKU 素材齐备率」：某个 SKU 在某平台上，
     * 当该平台要求的素材类型都具备时，记为已齐备。
     * </p>
     */
    @Override
    public List<SpaceCoverageAnalyzeResponse> getSpaceCoverageAnalyze(SpaceCoverageAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeAuth(request, loginUser);
        // 公共图库/全部空间不支持 SKU 维度分析（SKU 归属具体店铺）
        ThrowUtils.throwIf(request.isQueryAll() || request.isQueryPublic(),
                ErrorCode.PARAMS_ERROR, "素材覆盖率分析仅支持指定店铺");
        Long spaceId = request.getSpaceId();
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "请指定店铺");

        // 1. 查出店铺下所有 SKU
        List<Product> productList = productService.lambdaQuery()
                .eq(Product::getSpaceId, spaceId)
                .list();
        if (productList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查出店铺下所有带平台与素材类型的素材
        // 注意：picture 按 spaceId 动态分表，必须带 spaceId
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.select("productId", "platform", "assetType")
                .eq("spaceId", spaceId)
                .isNotNull("productId")
                .isNotNull("platform")
                .isNotNull("assetType");
        List<Picture> pictureList = pictureService.getBaseMapper().selectList(pictureQueryWrapper);

        // 3. 构造「平台 + SKU + 素材类型」已具备的集合
        Set<String> ownedSet = pictureList.stream()
                .map(p -> p.getPlatform() + "#" + p.getProductId() + "#" + p.getAssetType())
                .collect(Collectors.toSet());

        // 4. 逐平台统计
        List<SpaceCoverageAnalyzeResponse> result = new ArrayList<>();
        for (PlatformAssetRuleEnum rule : PlatformAssetRuleEnum.allRules()) {
            long skuCount = productList.size();
            long coveredCount = 0;
            for (Product product : productList) {
                boolean covered = rule.getRequiredAssetTypes().stream()
                        .allMatch(assetType -> ownedSet.contains(
                                rule.getPlatform() + "#" + product.getId() + "#" + assetType.getValue()));
                if (covered) {
                    coveredCount++;
                }
            }
            long missingCount = skuCount - coveredCount;
            double coverageRate = skuCount == 0 ? 0
                    : NumberUtil.round(coveredCount * 100.0 / skuCount, 2).doubleValue();
            result.add(new SpaceCoverageAnalyzeResponse(
                    rule.getPlatform(), rule.getPlatformName(),
                    skuCount, coveredCount, missingCount, coverageRate));
        }
        return result;
    }

    /**
     * 素材缺口分析
     * <p>
     * 按平台维度列出缺失的 SKU 明细，前端可直接触发 AI 生成补图。
     * </p>
     */
    @Override
    public List<SpaceMissingAnalyzeResponse> getSpaceMissingAnalyze(SpaceMissingAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeAuth(request, loginUser);
        ThrowUtils.throwIf(request.isQueryAll() || request.isQueryPublic(),
                ErrorCode.PARAMS_ERROR, "素材缺口分析仅支持指定店铺");
        Long spaceId = request.getSpaceId();
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "请指定店铺");

        // 1. 查出店铺下所有 SKU
        List<Product> productList = productService.lambdaQuery()
                .eq(Product::getSpaceId, spaceId)
                .list();
        if (productList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查出已具备的平台 + SKU + 素材类型组合
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.select("productId", "platform", "assetType")
                .eq("spaceId", spaceId)
                .isNotNull("productId")
                .isNotNull("platform")
                .isNotNull("assetType");
        List<Picture> pictureList = pictureService.getBaseMapper().selectList(pictureQueryWrapper);
        Set<String> ownedSet = pictureList.stream()
                .map(p -> p.getPlatform() + "#" + p.getProductId() + "#" + p.getAssetType())
                .collect(Collectors.toSet());

        // 3. 逐平台逐 SKU 找缺口
        List<SpaceMissingAnalyzeResponse> result = new ArrayList<>();
        for (PlatformAssetRuleEnum rule : PlatformAssetRuleEnum.allRules()) {
            for (Product product : productList) {
                for (AssetTypeEnum assetType : rule.getRequiredAssetTypes()) {
                    boolean owned = ownedSet.contains(
                            rule.getPlatform() + "#" + product.getId() + "#" + assetType.getValue());
                    if (!owned) {
                        result.add(new SpaceMissingAnalyzeResponse(
                                rule.getPlatform(), rule.getPlatformName(),
                                assetType.getValue(), assetType.getText(),
                                product.getId(), product.getSku(), product.getTitle()));
                    }
                }
            }
        }
        return result;
    }


    /**
     * 用户权限校验
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    public void checkAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //如果是查询公共图库或者全部空间必须为管理员
        if(spaceAnalyzeRequest.isQueryAll()||spaceAnalyzeRequest.isQueryPublic()){
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR,"无权访问公共空间");
        }else {
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.OPERATION_ERROR,"空间不存在");
            spaceService.checkSpaceAuth(loginUser,space);
        }
    }

    /**
     * 填充参数
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //如果全部空间 就不填充
        if(spaceAnalyzeRequest.isQueryAll()){
            return;
        }
        //如果是公共图库就设置spaceId为空
        if(spaceAnalyzeRequest.isQueryPublic()){
            queryWrapper.isNull("spaceId");
            return;
        }
        //查询spaceId的话就设置spaceId；
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if(spaceId!=null){
            queryWrapper.eq("spaceId",spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR,"未指定查询范围");

    }
}



