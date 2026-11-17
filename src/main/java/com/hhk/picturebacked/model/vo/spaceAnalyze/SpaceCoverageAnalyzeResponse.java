package com.hhk.picturebacked.model.vo.spaceAnalyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 素材覆盖率分析响应
 * 按平台维度统计素材齐备情况
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCoverageAnalyzeResponse implements Serializable {

    /**
     * 平台枚举值（AMAZON SHOPEE TIKTOK TEMU）
     */
    private String platform;

    /**
     * 平台中文名
     */
    private String platformName;

    /**
     * 该平台下的 SKU 总数
     */
    private Long skuCount;

    /**
     * 该平台下素材已齐备的 SKU 数
     */
    private Long coveredSkuCount;

    /**
     * 该平台下缺图的 SKU 数
     */
    private Long missingSkuCount;

    /**
     * 该平台素材齐备率（%），保留两位小数
     */
    private Double coverageRate;

    private static final long serialVersionUID = 1L;
}
