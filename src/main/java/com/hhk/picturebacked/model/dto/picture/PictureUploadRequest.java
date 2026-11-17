package com.hhk.picturebacked.model.dto.picture;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;
    /**
     * 图片地址
     */
    @JsonAlias({"fileUrl", "Url"})
    private String url;
    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 归属商品 id（SKU），为空表示未绑定
     */
    private Long productId;

    /**
     * 素材类型：MAIN / DETAIL / SCENE / WHITE / DETAIL_LONG
     */
    private String assetType;

    /**
     * 目标平台：AMAZON / SHOPEE / TIKTOK / TEMU / COMMON
     */
    private String platform;

    /**
     * 语言：zh / en
     */
    private String lang;

    /**
     * 上传标记，内部使用
     * 传 ai 表示走 AI 产物上传策略（体积上限放宽到 10MB）
     */
    private String uploadFlag;

    private static final long serialVersionUID = 1L;
}
