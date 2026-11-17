package com.hhk.picturebacked.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

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
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

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

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;

    /**
     * 归属商品 id（SKU），为空表示未绑定商品
     * 注意：picture 按 spaceId 动态分表，查询时必须带上 spaceId，避免全分片扫描
     */
    private Long productId;

    /**
     * 素材类型：MAIN-主图 DETAIL-细节 SCENE-场景 WHITE-白底 DETAIL_LONG-长图
     */
    private String assetType;

    /**
     * 目标平台：AMAZON SHOPEE TIKTOK TEMU COMMON
     */
    private String platform;

    /**
     * 来源原图 id，衍生图（AI 生成、平台规格图）指向原图
     */
    private Long sourceId;

    /**
     * 语言：zh en
     */
    private String lang;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 浏览量（由定时任务批量刷库写入）
     * 注意：本项目 mybatis-plus 关闭了下划线转驼峰（map-underscore-to-camel-case: false），
     * 这里显式指定列名，否则字段映射不上
     */
    @TableField("view_count")
    private Long viewCount;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}