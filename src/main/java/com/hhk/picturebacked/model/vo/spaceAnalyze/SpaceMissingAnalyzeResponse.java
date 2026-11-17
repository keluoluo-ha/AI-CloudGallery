package com.hhk.picturebacked.model.vo.spaceAnalyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 素材缺口分析响应
 * 按平台维度列出缺失主图的 SKU 明细
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceMissingAnalyzeResponse implements Serializable {

    /**
     * 平台枚举值
     */
    private String platform;

    /**
     * 平台中文名
     */
    private String platformName;

    /**
     * 缺失的素材类型（当前固定为主图）
     */
    private String assetType;

    /**
     * 素材类型中文名
     */
    private String assetTypeName;

    /**
     * 缺失的商品 id
     */
    private Long productId;

    /**
     * SKU 编码
     */
    private String sku;

    /**
     * 商品名称
     */
    private String title;

    private static final long serialVersionUID = 1L;
}
