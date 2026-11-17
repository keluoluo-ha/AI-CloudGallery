package com.hhk.picturebacked.model.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品（SKU）编辑请求
 */
@Data
public class ProductEditRequest implements Serializable {

    /**
     * 商品 id
     */
    private Long id;

    /**
     * SKU 编码，店铺内唯一
     */
    private String sku;

    /**
     * 商品名称
     */
    private String title;

    /**
     * 封面图片 id
     */
    private Long coverId;

    /**
     * 状态：0-草稿 1-在售 2-停售
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
