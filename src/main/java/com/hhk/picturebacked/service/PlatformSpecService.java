package com.hhk.picturebacked.service;

import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.vo.PlatformSpecVO;
import com.hhk.picturebacked.model.vo.SpecCheckResultVO;

import java.util.List;

/**
 * 平台规格服务
 * <p>
 * 提供四平台图片规格查询，以及图片是否符合平台规格的校验。
 * 校验结果用于：上传时提示、审核时卡点、下载时提醒。
 * </p>
 */
public interface PlatformSpecService {

    /**
     * 获取全部平台规格
     */
    List<PlatformSpecVO> listSpec();

    /**
     * 获取单个平台规格
     *
     * @param platform 平台枚举值
     */
    PlatformSpecVO getSpec(String platform);

    /**
     * 校验图片是否符合平台规格
     *
     * @param picture      图片
     * @param platform     平台枚举值
     * @param assetType    素材类型，决定用主图规格还是详情规格
     * @param productSku   商品 SKU，用于生成建议文件名
     */
    SpecCheckResultVO checkPicture(Picture picture, String platform, String assetType, String productSku);

    /**
     * 生成合规文件命名：SKU_平台_类型_序号.jpg
     *
     * @param productSku 商品 SKU
     * @param platform   平台枚举值
     * @param assetType  素材类型
     * @param sequence   序号
     */
    String buildFileName(String productSku, String platform, String assetType, int sequence);
}
