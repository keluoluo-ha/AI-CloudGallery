package com.hhk.picturebacked.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 图片名称  
     */  
    private String name;  
  
    /**  
     * 简介  
     */  
    private String introduction;  
  
    /**  
     * 分类  
     */  
    private String category;  
  
    /**
     * 标签
     */
    private List<String> tags;

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

    private static final long serialVersionUID = 1L;
}
