package com.hhk.picturebacked.model.dto.ecommerce;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 平台规格校验请求
 */
@Data
public class SpecCheckRequest implements Serializable {

    /**
     * 平台枚举值：AMAZON / SHOPEE / TIKTOK / TEMU
     */
    private String platform;

    /**
     * 素材类型：MAIN / DETAIL / SCENE / WHITE / DETAIL_LONG
     */
    private String assetType;

    /**
     * 待校验的图片 id 列表
     */
    private List<Long> pictureIdList;

    private static final long serialVersionUID = 1L;
}
