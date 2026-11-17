package com.hhk.picturebacked.model.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品（SKU）新增请求
 */
@Data
public class ProductAddRequest implements Serializable {

    /**
     * 所属空间 id（店铺）
     */
    private Long spaceId;

    /**
     * SKU 编码，店铺内唯一
     */
    private String sku;

    /**
     * 商品名称
     */
    private String title;

    /**
     * 状态：0-草稿 1-在售 2-停售
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
