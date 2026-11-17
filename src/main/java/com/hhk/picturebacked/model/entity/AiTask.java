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
 * AI 渲染任务
 * <p>
 * 提交即返回，异步消费 AI，产物落 picture 并回填 result_picture_id。
 * </p>
 *
 * @TableName ai_task
 */
@TableName(value = "ai_task")
@Data
public class AiTask implements Serializable {
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
     * 归属商品 id（SKU）
     */
    private Long productId;

    /**
     * 来源原图 id
     */
    private Long sourcePictureId;

    /**
     * 任务类型：REMOVE_BG / WHITE_BG / SCENE / RESIZE
     */
    private String type;

    /**
     * 目标平台：AMAZON / SHOPEE / TIKTOK / TEMU / COMMON
     */
    private String platform;

    /**
     * 产出的素材类型
     */
    private String assetType;

    /**
     * 场景模板标识
     */
    private String template;

    /**
     * 状态：0-排队中 1-处理中 2-成功 3-失败
     */
    private Integer status;

    /**
     * 第三方 AI 任务 id
     */
    private String aiTaskId;

    /**
     * 成功后生成的图片 id
     */
    private Long resultPictureId;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 任务参数快照
     */
    private String paramJson;

    /**
     * 幂等键：源图 + 类型 + 平台 + 模板
     */
    private String requestKey;

    /**
     * 提交用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 完成时间
     */
    private Date finishTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
