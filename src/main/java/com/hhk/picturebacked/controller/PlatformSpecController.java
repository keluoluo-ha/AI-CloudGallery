package com.hhk.picturebacked.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.hhk.picturebacked.auth.annotation.SaSpaceCheckPermission;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.commom.ResultUtils;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.model.dto.ecommerce.SpecCheckRequest;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.vo.PictureSpecCheckVO;
import com.hhk.picturebacked.model.vo.PlatformSpecVO;
import com.hhk.picturebacked.model.vo.SpecCheckResultVO;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.PlatformSpecService;
import com.hhk.picturebacked.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台规格：查询与合规校验
 */
@RestController
@RequestMapping("/platform/spec")
@Slf4j
public class PlatformSpecController {

    @Resource
    private PlatformSpecService platformSpecService;

    @Resource
    private PictureService pictureService;

    @Resource
    private ProductService productService;

    /**
     * 四平台规格列表
     */
    @GetMapping("/list")
    public BaseResponse<List<PlatformSpecVO>> listSpec() {
        return ResultUtils.success(platformSpecService.listSpec());
    }

    /**
     * 单个平台规格
     */
    @GetMapping("/get")
    public BaseResponse<PlatformSpecVO> getSpec(@RequestParam("platform") String platform) {
        ThrowUtils.throwIf(StrUtil.isBlank(platform), ErrorCode.PARAMS_ERROR);
        PlatformSpecVO spec = platformSpecService.getSpec(platform);
        ThrowUtils.throwIf(spec == null, ErrorCode.PARAMS_ERROR, "不支持的平台：" + platform);
        return ResultUtils.success(spec);
    }

    /**
     * 批量校验图片是否符合平台规格
     * <p>
     * 用于运营下载前自检、审核时辅助判断。
     * </p>
     */
    @PostMapping("/check")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureSpecCheckVO>> checkPictures(@RequestBody SpecCheckRequest specCheckRequest) {
        ThrowUtils.throwIf(specCheckRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(specCheckRequest.getPlatform()), ErrorCode.PARAMS_ERROR, "平台不能为空");
        List<Long> pictureIdList = specCheckRequest.getPictureIdList();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "图片列表不能为空");
        ThrowUtils.throwIf(pictureIdList.size() > 100, ErrorCode.PARAMS_ERROR, "一次最多校验 100 张");

        List<Picture> pictureList = pictureService.listByIds(pictureIdList);
        // 取商品 SKU 用于生成建议文件名
        Map<Long, Product> productMap = pictureList.stream()
                .map(Picture::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(productService::getById)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<PictureSpecCheckVO> resultList = pictureList.stream().map(picture -> {
            String sku = null;
            if (picture.getProductId() != null && productMap.containsKey(picture.getProductId())) {
                sku = productMap.get(picture.getProductId()).getSku();
            }
            SpecCheckResultVO result = platformSpecService.checkPicture(
                    picture, specCheckRequest.getPlatform(), specCheckRequest.getAssetType(), sku);
            return PictureSpecCheckVO.objToVo(picture, result);
        }).collect(Collectors.toList());

        return ResultUtils.success(resultList);
    }

    /**
     * 生成合规文件命名
     */
    @GetMapping("/file_name")
    public BaseResponse<String> buildFileName(@RequestParam("sku") String sku,
                                               @RequestParam("platform") String platform,
                                               @RequestParam(value = "assetType", required = false) String assetType,
                                               @RequestParam(value = "sequence", defaultValue = "1") int sequence) {
        return ResultUtils.success(platformSpecService.buildFileName(sku, platform, assetType, sequence));
    }
}
