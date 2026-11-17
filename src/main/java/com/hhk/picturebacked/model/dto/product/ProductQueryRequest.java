package com.hhk.picturebacked.model.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品（SKU）查询请求
 */
@Data
public class ProductQueryRequest implements Serializable {

    /**
     * 商品 id
     */
    private Long id;

    /**
     * 所属空间 id（店铺）
     */
    private Long spaceId;

    /**
     * SKU 编码，店铺内唯一
     */
    private String sku;

    /**
     * 商品名称，模糊匹配
     */
    private String title;

    /**
     * 状态：0-草稿 1-在售 2-停售
     */
    private Integer status;

    /**
     * 当前页码
     */
    private long current = 1;

    /**
     * 每页条数
     */
    private long pageSize = 20;

    private static final long serialVersionUID = 1L;
}
