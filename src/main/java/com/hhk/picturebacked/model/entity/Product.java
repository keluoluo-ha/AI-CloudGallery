package com.hhk.picturebacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品（SKU）
 * 一个 Space 对应一个店铺，一个 product 对应店铺下的一个 SKU
 *
 * @TableName product
 */
@TableName(value = "product")
@Data
public class Product implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
